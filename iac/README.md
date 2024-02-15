## Use CDKTF to deploy the infrastructure and the app

### Install CDKTF

* The [Terraform CLI](https://developer.hashicorp.com/terraform/tutorials/aws-get-started/install-cli) (1.2+).
* [Node.js](https://nodejs.org/en) and npm v16+.
* Java [OpenJDK v17](https://openjdk.java.net/) and [Gradle](https://gradle.org/install/)

__Note:__ The latest CDKTF for Java is using Gradle instead Maven, which provides faster synthesizing.


For more details, read the [Install CDKTF tutorial](https://developer.hashicorp.com/terraform/tutorials/cdktf/cdktf-install).

### Build the container images

The images need to be built and stored in a registry. For this application, you can build different types of images. The following commands provide an example:

```bash
# Assuming from the root directory of the project
ROOT_DIR=$(pwd)

cd $ROOT_DIR/services/audit
gcloud builds submit .  --machine-type E2-HIGHCPU-32 --config cloudbuild-native.yaml 

cd $ROOT_DIR/services/faulty
gcloud builds submit .  --machine-type E2-HIGHCPU-32 --config cloudbuild.yaml --substitutions=_TYPE=native

cd $ROOT_DIR/services/bff
gcloud builds submit .  --machine-type E2-HIGHCPU-32 --config cloudbuild.yaml --substitutions=_TYPE=native

cd $ROOT_DIR/services/reference
gcloud builds submit .  --machine-type E2-HIGHCPU-32 --config cloudbuild.yaml --substitutions=_TYPE=native

cd $ROOT_DIR/services/quotes
gcloud builds submit .  --machine-type E2-HIGHCPU-32 --config cloudbuild-native.yaml
```

### Deploy the infrastructure and the app

You can use different backends to manage the Terraform state. Here is an example using the GCS backend.

```bash
export PROJECT_ID=[Use your GCP project Id here]
export GCS_BACKEND_BUCKET_NAME=${PROJECT_ID}-cdktf-state
gcloud storage buckets create gs://${GCS_BACKEND_BUCKET_NAME}
```

Use the CDKTF CLI to deploy. Notice you can pass in various parameters for the deployment.
```bash
cd $ROOT_DIR/iac
cdktf deploy application-dev \
    --var='auditImageName=audit-native' \
    --var='referenceImageName=reference-native' \
    --var='bffImageName=bff-native' \
    --var='faultyImageName=faulty-native' \
    --var='quotesImageName=quotes-native' \
    --auto-approve
```

Wait for a few minutes and check the results. If the command runs successfully, you should see output like the following:
```terminal
...
bff-dev
      bff-service-loadbalancer = http://34.36.147.241
      bff-service-url = https://bff-service-uzog2g4wga-uc.a.run.app
...
```

Run the following command to get a JSON output:
```bash
curl http://[load balancer ip]/quotes
```
If you get an error like `Recv failure`, wait a few minutes and try again.

### Destroy

To destroy the deployment, you can run the following commands:
```bash
export PROJECT_ID=[Use your GCP project Id here]
export GCS_BACKEND_BUCKET_NAME=${PROJECT_ID}-cdktf-state
cdktf destroy application-dev --auto-approve
```

## Use scripts
Alternatively, you can use the scripts in this directory to automate some of the tasks.

1. Run the following script to build the container images. You can update the script for different types of images:
```bash
./build-images.sh
```

2. Use Cloud Build to deploy or destroy the resources:

Grant the required access to your Cloud Build service account:

```bash
CLOUDBUILD_SA="$(gcloud projects describe $PROJECT_ID \
    --format 'value(projectNumber)')@cloudbuild.gserviceaccount.com"
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member serviceAccount:$CLOUDBUILD_SA --role roles/editor
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member serviceAccount:$CLOUDBUILD_SA --role roles/secretmanager.admin
gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member serviceAccount:$CLOUDBUILD_SA --role roles/servicenetworking.networksAdmin
```

Deploy the resources:
```bash
gcloud builds submit --config=cloudbuild.yaml \
  --substitutions=_CDKTF_TYPE="deploy"
```

Destroy the resources:
```bash
gcloud builds submit --config=cloudbuild.yaml \
  --substitutions=_CDKTF_TYPE="destroy"
```