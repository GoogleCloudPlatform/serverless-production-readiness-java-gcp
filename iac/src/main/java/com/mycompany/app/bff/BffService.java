package com.mycompany.app.bff;

import java.util.List;
import com.hashicorp.cdktf.TerraformOutput;
import com.hashicorp.cdktf.providers.google.compute_url_map.ComputeUrlMap;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2Service;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplate;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplateContainers;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplateContainersEnv;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplateContainersResources;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service_iam_policy.CloudRunV2ServiceIamPolicy;
import com.hashicorp.cdktf.providers.google.compute_backend_service.ComputeBackendService;
import com.hashicorp.cdktf.providers.google.compute_backend_service.ComputeBackendServiceBackend;
import com.hashicorp.cdktf.providers.google.compute_global_address.ComputeGlobalAddress;
import com.hashicorp.cdktf.providers.google.compute_global_forwarding_rule.ComputeGlobalForwardingRule;
import com.hashicorp.cdktf.providers.google.compute_region_network_endpoint_group.ComputeRegionNetworkEndpointGroup;
import com.hashicorp.cdktf.providers.google.compute_region_network_endpoint_group.ComputeRegionNetworkEndpointGroupCloudRun;
import com.hashicorp.cdktf.providers.google.compute_target_http_proxy.ComputeTargetHttpProxy;
import com.hashicorp.cdktf.providers.google.data_google_iam_policy.DataGoogleIamPolicy;
import com.hashicorp.cdktf.providers.google.data_google_iam_policy.DataGoogleIamPolicyBinding;
import com.mycompany.app.ApplicationConfig;
import software.constructs.Construct;

public class BffService extends Construct {

    private String svcUrl;

    public String getSvcUrl() {
        return this.svcUrl;
    }

    public BffService(Construct scope, String id, ApplicationConfig config, String refUrl,
            String quotesUrl, String faultyUrl, String imageName) {
        super(scope, id);

        String project = config.getProject();
        String region = config.getRegion();

        CloudRunV2ServiceTemplateContainersEnv quotesSvcUrl = CloudRunV2ServiceTemplateContainersEnv
                .builder().name("QUOTES_URL").value(quotesUrl).build();
        CloudRunV2ServiceTemplateContainersEnv refSvcUrl = CloudRunV2ServiceTemplateContainersEnv
                .builder().name("REFERENCE_URL").value(refUrl).build();
        CloudRunV2ServiceTemplateContainersEnv faultySvcUrl = CloudRunV2ServiceTemplateContainersEnv
                .builder().name("FAULTY_URL").value(faultyUrl).build();

        CloudRunV2Service cr = CloudRunV2Service.Builder.create(this, "bff-cr-service")
                .name("bff-service").project(project).location(region)
                .ingress("INGRESS_TRAFFIC_INTERNAL_LOAD_BALANCER")
                .template(CloudRunV2ServiceTemplate.builder()
                        .containers(List.of(CloudRunV2ServiceTemplateContainers.builder()
                                .env(List.of(quotesSvcUrl, refSvcUrl, faultySvcUrl))
                                .image(imageName)
                                .resources(CloudRunV2ServiceTemplateContainersResources.builder()
                                        .limits(config.getMemory()).build())
                                .build()))
                        .build())
                .build();

        DataGoogleIamPolicy crPolicy =
                DataGoogleIamPolicy.Builder.create(this, "datanoauth")
                        .binding(List.of(DataGoogleIamPolicyBinding.builder()
                                .role("roles/run.invoker").members(List.of("allUsers")).build()))
                        .build();

        CloudRunV2ServiceIamPolicy.Builder.create(this, "bff-policy").location(cr.getLocation())
                .project(cr.getProject()).name(cr.getName()).policyData(crPolicy.getPolicyData())
                .build();

        this.svcUrl = cr.getUri();
        TerraformOutput.Builder.create(this, "bff-service-url").value(svcUrl).build();

        // Get an IP
        ComputeGlobalAddress ip =
                ComputeGlobalAddress.Builder.create(this, "bff-ip").name("bff-ip").build();
        // Create a serverless NEG for the service
        ComputeRegionNetworkEndpointGroup neg =
                ComputeRegionNetworkEndpointGroup.Builder.create(this, "bff-neg").name("bff-neg")
                        .region(region).networkEndpointType("SERVERLESS")
                        .cloudRun(ComputeRegionNetworkEndpointGroupCloudRun.builder()
                                .service(cr.getName()).build())
                        .build();
        // Create the backend service
        ComputeBackendService bffsvc = ComputeBackendService.Builder.create(this, "bff-backend-svc")
                .name("bff-backend-svc").timeoutSec(30).loadBalancingScheme("EXTERNAL")
                .backend(List.of(ComputeBackendServiceBackend.builder().group(neg.getId())
                        .balancingMode("UTILIZATION").capacityScaler(1.0).build()))
                .build();
        // Create the URL map
        ComputeUrlMap urlmap = ComputeUrlMap.Builder.create(this, "bff-url-map").name("bff-url-map")
                .defaultService(bffsvc.getId()).build();
        // Create a proxy
        ComputeTargetHttpProxy httpProxy =
                ComputeTargetHttpProxy.Builder.create(this, "bff-http-proxy").name("bff-http-proxy")
                        .urlMap(urlmap.getId()).build();
        // Create a forwarding rule
        ComputeGlobalForwardingRule.Builder.create(this, "bff-foward-rule").name("bff-forward-rule")
                .loadBalancingScheme("EXTERNAL").ipAddress(ip.getAddress())
                .target(httpProxy.getId()).portRange("80").build();

        TerraformOutput.Builder.create(this, "bff-service-loadbalancer")
                .value("http://" + ip.getAddress()).build();
    }
}
