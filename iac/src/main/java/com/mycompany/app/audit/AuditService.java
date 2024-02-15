package com.mycompany.app.audit;

import java.util.List;
import com.hashicorp.cdktf.TerraformOutput;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2Service;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplate;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplateContainers;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplateContainersResources;
import com.hashicorp.cdktf.providers.google.project_service.ProjectService;
import com.mycompany.app.ApplicationConfig;
import software.constructs.Construct;

public class AuditService extends Construct {

    private String svcUrl;

    public String getSvcUrl() {
        return this.svcUrl;
    }

    public AuditService(Construct scope, String id, ApplicationConfig config, String imageName) {
        super(scope, id);

        String project = config.getProject();
        String region = config.getRegion();

        ProjectService.Builder.create(this, "enableFirestoreService").disableOnDestroy(false)
                .project(project).service("firestore.googleapis.com").build();

        CloudRunV2Service cr = CloudRunV2Service.Builder.create(this, "audit-cr-service")
                .name("audit-service").project(project).location(region)
                .template(CloudRunV2ServiceTemplate.builder()
                        .containers(List.of(CloudRunV2ServiceTemplateContainers.builder()
                                .image(imageName)
                                .resources(CloudRunV2ServiceTemplateContainersResources.builder()
                                        .limits(config.getMemory()).build())
                                .build()))
                        .build())
                .build();

        this.svcUrl = cr.getUri();
        TerraformOutput.Builder.create(this, "audit-service-url").value(svcUrl).build();
    }
}
