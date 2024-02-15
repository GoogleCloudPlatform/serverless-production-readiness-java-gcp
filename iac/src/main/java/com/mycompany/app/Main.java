package com.mycompany.app;

import com.hashicorp.cdktf.*;

public class Main {
    public static void main(String[] args) {
        final App app = new App();

        // Choose a backend to store state. Default preference is a local file, GCS and remote.
        // See more details at:
        // https://developer.hashicorp.com/terraform/cdktf/concepts/remote-backends
        // Default to use a GCS bucket to store state. You need to pre-create the bucket and
        // provide the bucket name.
        // If use a remote bakcend, you need to update the configurations such as organization
        // and workspace.
        // If you need to change backends, refer to
        // https://developer.hashicorp.com/terraform/cdktf/concepts/remote-backends#migrate-local-state-storage-to-remote

        String backend = "LOCAL";
        String bucketName = null;
        if (System.getenv("GCS_BACKEND_BUCKET_NAME") != null) {
            backend = "GCS";
            bucketName = System.getenv("GCS_BACKEND_BUCKET_NAME");
        } else if ("true".equals(System.getenv("USE_REMOTE_BACKEND"))) {
            backend = "REMTOE";
        }

        // Get current GCP project id and region from the environment
        String projectId = System.getenv("PROJECT_ID");
        if (projectId == null) {
            throw new RuntimeException("PROJECT_ID environment variable not set");
        }

        String region = System.getenv("REGION");
        if (region == null) {
            System.out.println(
                    "Cloud Region is not found in environment variable. Defaulting to us-central1");
            region = "us-central1";
        }

        ApplicationConfig appDevConfig = new ApplicationConfig("dev", projectId, region);
        ApplicationStack devStack = new ApplicationStack(app, "application-dev", appDevConfig);
        if ("GCS".equals(backend)) {
            GcsBackend.Builder.create(devStack).bucket(bucketName).prefix("terraform/state")
                    .build();
        } else if (!"LOCAL".equals(backend) && "REMOTE".equals(backend)) {
            new RemoteBackend(devStack, RemoteBackendConfig.builder()
                    .organization("terraform-demo-serverless")
                    .workspaces(
                            new NamedRemoteWorkspace("cdktf-integration-serverless-java-example"))
                    .build());
        }

        ApplicationConfig appProdConfig = new ApplicationConfig("prod", projectId, region);
        ApplicationStack prodStack = new ApplicationStack(app, "application-prod", appProdConfig);
        if ("GCS".equals(backend)) {
            GcsBackend.Builder.create(prodStack).bucket(bucketName).prefix("terraform/state")
                    .build();
        } else if (backend != "LOCAL" && backend == "REMOTE") {
            new RemoteBackend(devStack, RemoteBackendConfig.builder()
                    .organization("terraform-demo-serverless")
                    .workspaces(
                            new NamedRemoteWorkspace("cdktf-integration-serverless-java-example"))
                    .build());
        }

        app.synth();
    }
}
