package com.mycompany.app;

import software.constructs.Construct;
import java.util.List;
import com.hashicorp.cdktf.TerraformStack;
import com.hashicorp.cdktf.TerraformVariable;
import com.hashicorp.cdktf.TerraformVariableConfig;
import com.mycompany.app.audit.AuditService;
import com.mycompany.app.bff.BffService;
import com.mycompany.app.faulty.FaultyService;
import com.mycompany.app.quotes.QuotesService;
import com.mycompany.app.reference.ReferenceService;
import com.hashicorp.cdktf.providers.docker.provider.DockerProvider;
import com.hashicorp.cdktf.providers.docker.provider.DockerProviderRegistryAuth;
import com.hashicorp.cdktf.providers.google.compute_network.ComputeNetwork;
import com.hashicorp.cdktf.providers.google.project_service.ProjectService;
import com.hashicorp.cdktf.providers.google.provider.GoogleProvider;
import com.hashicorp.cdktf.providers.google.vpc_access_connector.VpcAccessConnector;
import com.hashicorp.cdktf.providers.random_provider.provider.RandomProvider;

public class ApplicationStack extends TerraformStack {

    public ApplicationStack(Construct scope, String name, ApplicationConfig appConfig) {
        super(scope, name);

        String environment = appConfig.getEnvironment();
        String project = appConfig.getProject();
        String region = appConfig.getRegion();
        String imagePrefix = appConfig.getImagePrefix();

        // Initialize the providers
        GoogleProvider.Builder.create(this, "google-cloud").region(region).project(project).build();
        RandomProvider.Builder.create(this, "radnom-provider").build();
        DockerProvider.Builder.create(this, "docker")
                .registryAuth(
                        List.of(DockerProviderRegistryAuth.builder().address("gcr.io").build()))
                .build();

        // Enable the cloud services
        ProjectService.Builder.create(this, "enableCloudRun").disableOnDestroy(false)
                .project(project).service("run.googleapis.com").build();
        ProjectService.Builder.create(this, "enableContainerRegistry").disableOnDestroy(false)
                .project(project).service("containerregistry.googleapis.com").build();
        ProjectService.Builder.create(this, "enableCompute").disableOnDestroy(false)
                .project(project).service("compute.googleapis.com").build();
        ProjectService.Builder.create(this, "enableVpcaccess").disableOnDestroy(false)
                .project(project).service("vpcaccess.googleapis.com").build();

        // Get the image names if they pass in as TF variables
        TerraformVariable referenceImageName = new TerraformVariable(this, "referenceImageName",
                TerraformVariableConfig.builder().type("string").defaultValue("reference")
                        .description("Container image name for the reference service").build());

        TerraformVariable quotesImageName = new TerraformVariable(this, "quotesImageName",
                TerraformVariableConfig.builder().type("string").defaultValue("quotes")
                        .description("Container image name for the quotes service").build());

        TerraformVariable faultyImageName = new TerraformVariable(this, "faultyImageName",
                TerraformVariableConfig.builder().type("string").defaultValue("faulty")
                        .description("Container image name for the faulty service").build());

        TerraformVariable auditImageName = new TerraformVariable(this, "auditImageName",
                TerraformVariableConfig.builder().type("string").defaultValue("audit")
                        .description("Container image name for the audit service").build());

        TerraformVariable bffImageName = new TerraformVariable(this, "bffImageName",
                TerraformVariableConfig.builder().type("string").defaultValue("bff")
                        .description("Container image name for the bff service").build());

        // Create the VPC
        ComputeNetwork serviceVpc = ComputeNetwork.Builder.create(this, "service-vpc")
                .name("service-vpc").autoCreateSubnetworks(false).build();
        appConfig.setServiceVpc(serviceVpc);
;
        // Create the VPC connector for Cloud Run services
        VpcAccessConnector connector = VpcAccessConnector.Builder.create(this, "vpcConnector")
                .name("vpc-connector").region(region).network(serviceVpc.getId())
                .ipCidrRange("10.128.0.0/28").build();
        appConfig.setConnector(connector);

        // Deploy the services
        ReferenceService refSvc = new ReferenceService(this, "reference-" + environment, appConfig,
                imagePrefix + referenceImageName.getStringValue());

        QuotesService quotesSvc = new QuotesService(this, "quotes-" + environment, appConfig,
                imagePrefix + quotesImageName.getStringValue());

        FaultyService faultySvc = new FaultyService(this, "faulty-" + environment, appConfig,
                imagePrefix + faultyImageName.getStringValue());

        new AuditService(this, "audit-" + environment, appConfig,
                imagePrefix + auditImageName.getStringValue());
        new BffService(this, "bff-" + environment, appConfig, refSvc.getSvcUrl(),
                quotesSvc.getSvcUrl(), faultySvc.getSvcUrl(),
                imagePrefix + bffImageName.getStringValue());
    }
}
