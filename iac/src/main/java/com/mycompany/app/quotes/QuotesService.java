package com.mycompany.app.quotes;

import java.util.List;
import com.hashicorp.cdktf.TerraformOutput;
import com.hashicorp.cdktf.TerraformVariable;
import com.hashicorp.cdktf.TerraformVariableConfig;
import com.hashicorp.cdktf.providers.google.data_google_project.DataGoogleProject;
import com.hashicorp.cdktf.providers.random_provider.password.Password;
import com.mycompany.app.ApplicationConfig;
import com.hashicorp.cdktf.providers.google.secret_manager_secret_iam_member.SecretManagerSecretIamMember;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2Service;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplate;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplateContainers;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplateContainersEnv;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplateContainersEnvValueSource;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplateContainersEnvValueSourceSecretKeyRef;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplateContainersResources;
import com.hashicorp.cdktf.providers.google.cloud_run_v2_service.CloudRunV2ServiceTemplateVpcAccess;
import com.hashicorp.cdktf.providers.google.compute_global_address.ComputeGlobalAddress;
import com.hashicorp.cdktf.providers.google.compute_network.ComputeNetwork;
import com.hashicorp.cdktf.providers.google.sql_database.SqlDatabase;
import com.hashicorp.cdktf.providers.google.sql_database_instance.SqlDatabaseInstance;
import com.hashicorp.cdktf.providers.google.sql_database_instance.SqlDatabaseInstanceSettings;
import com.hashicorp.cdktf.providers.google.sql_database_instance.SqlDatabaseInstanceSettingsIpConfiguration;
import com.hashicorp.cdktf.providers.google.sql_user.SqlUser;
import com.hashicorp.cdktf.providers.google.vpc_access_connector.VpcAccessConnector;
import com.hashicorp.cdktf.providers.google.project_service.ProjectService;
import com.hashicorp.cdktf.providers.google.secret_manager_secret.SecretManagerSecret;
import com.hashicorp.cdktf.providers.google.secret_manager_secret.SecretManagerSecretReplication;
import com.hashicorp.cdktf.providers.google.secret_manager_secret_version.SecretManagerSecretVersion;
import com.hashicorp.cdktf.providers.google.service_networking_connection.ServiceNetworkingConnection;
import software.constructs.Construct;

public class QuotesService extends Construct {

    private String svcUrl;

    public String getSvcUrl() {
        return this.svcUrl;
    }

    public QuotesService(Construct scope, String id, ApplicationConfig config, String imageName) {
        super(scope, id);

        String project = config.getProject();
        String region = config.getRegion();
        ComputeNetwork serviceVpc = config.getServiceVpc();
        VpcAccessConnector connector = config.getConnector();

        // Initialize the parameters
        TerraformVariable dbInstnaceSize = new TerraformVariable(this, "dbInstnaceSize",
                TerraformVariableConfig.builder().type("string").defaultValue("db-f1-micro")
                        .description("Cloud SQL instnace type").build());
        TerraformVariable databaseVersion = new TerraformVariable(this, "databaseVersion",
                TerraformVariableConfig.builder().type("string").defaultValue("POSTGRES_15")
                        .description("Cloud SQL PostGres version").build());
        TerraformVariable dbUser = new TerraformVariable(this, "dbUser",
                TerraformVariableConfig.builder().type("string").defaultValue("user")
                        .description("Cloud SQL DB user").build());
        TerraformVariable dbName = new TerraformVariable(this, "dbName",
                TerraformVariableConfig.builder().type("string").defaultValue("quote_db")
                        .description("Cloud SQL DB name").build());

        ProjectService sqlAdminService =
                ProjectService.Builder.create(this, "enableSqlAdminService").disableOnDestroy(false)
                        .project(project).service("sqladmin.googleapis.com").build();


        ComputeGlobalAddress privateIp = ComputeGlobalAddress.Builder.create(this, "privateIp")
                .name("private-ip").purpose("VPC_PEERING").addressType("INTERNAL").prefixLength(16)
                .network(serviceVpc.getId()).build();
        ServiceNetworkingConnection serviceNetworkingConnection =
                ServiceNetworkingConnection.Builder.create(this, "serviceNetworkingConnection")
                        .network(serviceVpc.getId())
                        .reservedPeeringRanges(List.of(privateIp.getName()))
                        .service("servicenetworking.googleapis.com").build();

        // Create the DB instance
        SqlDatabaseInstance sqlDBinstnace = SqlDatabaseInstance.Builder
                .create(this, "quotesDBinstance").name("serverless-db-instance").region(region)
                .databaseVersion(databaseVersion.getStringValue()).deletionProtection(false)
                .settings(SqlDatabaseInstanceSettings.builder()
                        .tier(dbInstnaceSize.getStringValue())
                        .ipConfiguration(SqlDatabaseInstanceSettingsIpConfiguration.builder()
                                .enablePrivatePathForGoogleCloudServices(true)
                                .privateNetwork(serviceVpc.getId()).ipv4Enabled(false).build())
                        .build())
                .dependsOn(List.of(sqlAdminService, serviceNetworkingConnection)).build();



        // Create the DB
        SqlDatabase.Builder.create(this, "quotesDb").name(dbName.getStringValue())
                .instance(sqlDBinstnace.getName()).build();

        // Create the DB password and store it in the secret manager
        Password pw = Password.Builder.create(this, "random-pw").length(12).build();

        SecretManagerSecret dbSecret =
                SecretManagerSecret.Builder.create(this, "db-secret").secretId("db-secret")
                        .replication(
                                SecretManagerSecretReplication.builder().automatic(true).build())
                        .build();
        SecretManagerSecretVersion.Builder.create(this, "db-pass").secret(dbSecret.getId())
                .secretData(pw.getResult()).build();

        // Create the SQL user
        SqlUser.Builder.create(this, "sqlUser").name(dbUser.getStringValue())
                .deletionPolicy("ABANDON").password(pw.getResult())
                .instance(sqlDBinstnace.getName()).build();

        DataGoogleProject deployProject =
                DataGoogleProject.Builder.create(this, "project").projectId(project).build();

        SecretManagerSecretIamMember.Builder
                .create(this, "secret-access").secretId(dbSecret.getSecretId())
                .role("roles/secretmanager.secretAccessor").member("serviceAccount:"
                        + deployProject.getNumber() + "-compute@developer.gserviceaccount.com")
                .build();

        // Set the environment variables for the Cloud Run job
        // CloudRunV2JobTemplateTemplateContainersEnv jobEnvSpringActiveProfile =
        // CloudRunV2JobTemplateTemplateContainersEnv.builder().name("SPRING_PROFILES_ACTIVE")
        // .value("cloud-" + config.getEnvironment()).build();
        // CloudRunV2JobTemplateTemplateContainersEnv jobEnvDbHost =
        // CloudRunV2JobTemplateTemplateContainersEnv.builder().name("DB_HOST")
        // .value(sqlDBinstnace.getFirstIpAddress()).build();
        // CloudRunV2JobTemplateTemplateContainersEnv jobEnvDbName =
        // CloudRunV2JobTemplateTemplateContainersEnv.builder().name("DB_DATABASE")
        // .value(dbName.getStringValue()).build();
        // CloudRunV2JobTemplateTemplateContainersEnv jobEnvDbUser =
        // CloudRunV2JobTemplateTemplateContainersEnv.builder().name("DB_USERT")
        // .value(dbUser.getStringValue()).build();
        // CloudRunV2JobTemplateTemplateContainersEnv jobEnvDbPasswd =
        // CloudRunV2JobTemplateTemplateContainersEnv.builder().name("DB_PASS")
        // .valueSource(CloudRunV2JobTemplateTemplateContainersEnvValueSource.builder()
        // .secretKeyRef(
        // CloudRunV2JobTemplateTemplateContainersEnvValueSourceSecretKeyRef
        // .builder().secret(dbSecret.getSecretId())
        // .version("latest").build())
        // .build())
        // .build();
        // Deploy a Cloud Run job to migrate the database schema

        // CloudRunJobExec crjobexec = CloudRunJobExec.Builder.create(this, "quotes-cr-job")
        // .name("quotes-cr-job").image(imageName).projectId(project).location(region)
        // .argument(null)
        // .envVars(List.of(Map.of("name", "DB_HOST", "value", sqlDBinstnace.getFirstIpAddress()),
        // Map.of("name", "DB_DATABASE", "value", dbName.getStringValue()),
        // Map.of("name", "DB_USER", "value", dbUser.getStringValue()),
        // Map.of("name", "DB_PASS", "value", pw.getResult())))
        // // .exec(true)
        // .dependsOn(List.of(quotesDb, dbSecret, user)).build();


        // CloudRunV2Job crJob = CloudRunV2Job.Builder.create(this,
        // "quotes-cr-job").name("quotes-job")
        // .project(project).location(region)
        // .template(CloudRunV2JobTemplate.builder().template(CloudRunV2JobTemplateTemplate
        // .builder()
        // .containers(List.of(CloudRunV2JobTemplateTemplateContainers.builder()
        // .image(imageName)
        // .resources(CloudRunV2JobTemplateTemplateContainersResources
        // .builder().limits(Map.of("memory", "2Gi")).build())
        // .env(List.of(jobEnvSpringActiveProfile, jobEnvDbHost, jobEnvDbName,
        // jobEnvDbUser, jobEnvDbPasswd))
        // .build()))
        // .build()).taskCount(1).build())


        // Set the environment variables for the Cloud Run service
        CloudRunV2ServiceTemplateContainersEnv envSpringActiveProfile =
                CloudRunV2ServiceTemplateContainersEnv.builder().name("SPRING_PROFILES_ACTIVE")
                        .value("cloud-" + config.getEnvironment()).build();
        CloudRunV2ServiceTemplateContainersEnv envDbHost = CloudRunV2ServiceTemplateContainersEnv
                .builder().name("DB_HOST").value(sqlDBinstnace.getFirstIpAddress()).build();
        CloudRunV2ServiceTemplateContainersEnv envDbName = CloudRunV2ServiceTemplateContainersEnv
                .builder().name("DB_DATABASE").value(dbName.getStringValue()).build();
        CloudRunV2ServiceTemplateContainersEnv envDbUser = CloudRunV2ServiceTemplateContainersEnv
                .builder().name("DB_USERT").value(dbUser.getStringValue()).build();
        CloudRunV2ServiceTemplateContainersEnv envDbPasswd = CloudRunV2ServiceTemplateContainersEnv
                .builder().name("DB_PASS")
                .valueSource(CloudRunV2ServiceTemplateContainersEnvValueSource.builder()
                        .secretKeyRef(CloudRunV2ServiceTemplateContainersEnvValueSourceSecretKeyRef
                                .builder().secret(dbSecret.getSecretId()).version("latest").build())
                        .build())
                .build();
        // Deploy the Cloud Run service
        CloudRunV2Service cr = CloudRunV2Service.Builder.create(this, "quotes-cr-service")
                .name("quotes-service").project(project).location(region)
                .template(CloudRunV2ServiceTemplate.builder()
                        .vpcAccess(CloudRunV2ServiceTemplateVpcAccess.builder()
                                .connector(connector.getId()).build())
                        .containers(List
                                .of(CloudRunV2ServiceTemplateContainers.builder().image(imageName)
                                        .resources(CloudRunV2ServiceTemplateContainersResources
                                                .builder().limits(config.getMemory()).build())
                                        .env(List.of(envSpringActiveProfile, envDbHost, envDbName,
                                                envDbUser, envDbPasswd))
                                        .build()))
                        .build())
                // .dependsOn(List.of(crjobexec))
                .build();

        this.svcUrl = cr.getUri();
        TerraformOutput.Builder.create(this, "quotes-service-url").value(svcUrl).build();
    }
}
