# Image Analysis - Cloud Run Service using Vision API, Vertex AI and Langchain4j

This lab can be executed directly in a Cloud Workstation, Cloudshell or your environment of choice. 

  [![Open in Cloud Shell](https://gstatic.com/cloudssh/images/open-btn.svg)](https://shell.cloud.google.com/cloudshell/editor?cloudshell_git_repo=https://github.com/GoogleCloudPlatform/serverless-photosharing-workshop.git)

## Setup Java ecosystem
In order to build JIT or Native Java app images, please set up Java and GraalVM and the associated Java 21 distributions.
A simple installer is available from [SDKMan](https://sdkman.io/install)
```shell
sdk install java 21.0.1-graal
```

## Clone the code:
```shell
git clone https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp.git
cd genai/image-vision-vertex-langchain/

git checkout java21
```

## Install the maven wrapper
The Maven Wrapper is an easy way to ensure a user of your Maven build has everything necessary to run your Maven build.

Run the command:
```shell
mvn wrapper:wrapper
```

## Build the service code and publish images to the container registry
Build the JIT app image:
```shell
./mvnw package

Start the app locally:
java -jar target/image-analysis-1.0.0.jar 
```

Build the Native Java executable: 
```shell
./mvnw native:compile -Pnative

Test the executable locally:
./target/image-analysis
```

### Build a JIT and Native Java container Image
```shell
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=image-analysis-jit

./mvnw spring-boot:build-image  -DskipTests -Pnative -Dspring-boot.build-image.imageName=image-analysis-native
```

Check the Docker image sizes:
```shell
docker images | grep image-analysis
image-analysis-native                              latest                2a8fdab9be12   43 years ago    360MB
image-analysis-jit                                 latest                1251d9e6a099   43 years ago    485MB
```

Start the Docker images locally. The image naming conventions indicate whether the image was built by Maven|Gradle and contains the JIT|NATIVE version
```shell
docker run --rm image-analysis-jit
docker run --rm image-analysis-native
```

Retrieve the Project ID, as it will be required for the next GCP operations
```shell
export PROJECT_ID=$(gcloud config get-value project)
echo $PROJECT_ID
```

Tag and push the images to GCR:
```shell
docker tag image-analysis-jit gcr.io/${PROJECT_ID}/image-analysis-jit
docker tag image-analysis-native gcr.io/${PROJECT_ID}/image-analysis-native

docker push gcr.io/${PROJECT_ID}/image-analysis-jit
docker push gcr.io/${PROJECT_ID}/image-analysis-native
```

## Deploy and run workshop code

Enable the required APIs, if they are not already enabled:
```shell
gcloud services enable vision.googleapis.com
gcloud services enable cloudfunctions.googleapis.com
gcloud services enable cloudbuild.googleapis.com 
gcloud services enable run.googleapis.com
gcloud services enable logging.googleapis.com 
gcloud services enable storage-component.googleapis.com 
gcloud services enable aiplatform.googleapis.com
```

Retrieve the Project ID and Project Number
```shell
# get the Project_ID
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
  or 
export PROJECT_ID=$(gcloud config get-value project)

# get the Project_Number
export PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')
```

Create the GCS bucket
```shell
export BUCKET_PICTURES=vision-${PROJECT_ID}
gsutil mb -l us-central1 gs://${BUCKET_PICTURES}
gsutil uniformbucketlevelaccess set on gs://${BUCKET_PICTURES}
gsutil iam ch allUsers:objectViewer gs://${BUCKET_PICTURES}
```

## Create the database
Instructions for configuring cloud Firestore available [here](https://codelabs.developers.google.com/codelabs/cloud-picadaily-lab1?hl=en&continue=https%3A%2F%2Fcodelabs.developers.google.com%2Fserverless-workshop%2F#8)

Set config variables
```shell
gcloud config set project ${PROJECT_ID}
gcloud config set run/region 
gcloud config set run/platform managed
gcloud config set eventarc/location us-central1
```

Grant `pubsub.publisher` to Cloud Storage service account
```shell
SERVICE_ACCOUNT="$(gsutil kms serviceaccount -p ${PROJECT_ID})"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role='roles/pubsub.publisher'
```

Deploy to Cloud Run
```shell
# deploy JIT image to Cloud Run
gcloud run deploy image-analysis-jit \
     --image gcr.io/${PROJECT_ID}/image-analysis-jit \
     --region us-central1 \
     --memory 2Gi --allow-unauthenticated

# deploy native Java image to Cloud Run

gcloud run deploy image-analysis-native \
     --image gcr.io/${PROJECT_ID}/image-analysis-native \
     --region us-central1 \
     --memory 2Gi --allow-unauthenticated  
```

Set up Eventarc triggers
```shell
gcloud eventarc triggers list --location=us-central1

gcloud eventarc triggers create image-analysis-jit-trigger \
     --destination-run-service=image-analysis-jit \
     --destination-run-region=us-central1 \
     --location=us-central1 \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=vision-${PROJECT_ID}" \
     --service-account=${PROJECT_NUMBER}-compute@developer.gserviceaccount.com

gcloud eventarc triggers create image-analysis-native-trigger \
     --destination-run-service=image-analysis-native \
     --destination-run-region=us-central1 \
     --location=us-central1 \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=vision-${PROJECT_ID}" \
     --service-account=${PROJECT_NUMBER}-compute@developer.gserviceaccount.com     
```

Test the trigger
```shell
gsutil cp images/GeekHour.jpeg gs://vision-${PROJECT_ID}
gsutil cp images/CloudRun.png gs://vision-${PROJECT_ID}

gcloud logging read "resource.labels.service_name=image-analysis-jit AND textPayload:GeekHour" --format=json
gcloud logging read "resource.labels.service_name=image-analysis-jit AND textPayload:CloudRun" --format=json
```

Log capture:
```shell
 gcloud logging read "resource.labels.service_name=image-analysis-jit AND textPayload:CloudRun" --format=json

...
  {
    "insertId": "65552700000bcf6d651346d3",
    "labels": {
      "instanceId": "0037d6d5d39b2d0440dde92c89a4a04cb82902d8919431fbd68fee17b20526b1e11b50283fe93ae67737ce6658a595469db6ad8fdca8225fb929b945115a2006"
    },
    "logName": "projects/optimize-serverless-apps/logs/run.googleapis.com%2Fstdout",
    "receiveTimestamp": "2023-11-15T20:16:00.824511791Z",
    "resource": {
      "labels": {
        "configuration_name": "image-analysis-jit",
        "location": "us-central1",
        "project_id": "optimize-serverless-apps",
        "revision_name": "image-analysis-jit-00001-z7g",
        "service_name": "image-analysis-jit"
      },
      "type": "cloud_run_revision"
    },
    "textPayload": "id : vision-optimize-serverless-apps/CloudRun.png/1700079343391581",
    "timestamp": "2023-11-15T20:16:00.773997Z"
  },

...
```

Log capture - Console
```
2023-11-15 15:16:08.518 EST
2023-11-15T20:16:08.518Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : - Brand
2023-11-15 15:16:08.518 EST
2023-11-15T20:16:08.518Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : - Sign
2023-11-15 15:16:08.519 EST
2023-11-15T20:16:08.519Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : Color: #1c82f5
2023-11-15 15:16:08.519 EST
2023-11-15T20:16:08.519Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : Is Image Safe? true
2023-11-15 15:16:08.519 EST
2023-11-15T20:16:08.519Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : Logo Annotations:
2023-11-15 15:16:08.519 EST
2023-11-15T20:16:08.519Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : Logo: Google
2023-11-15 15:16:08.519 EST
2023-11-15T20:16:08.519Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : Logo property list:
2023-11-15 15:16:08.519 EST
2023-11-15T20:16:08.519Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : Text Annotations:
2023-11-15 15:16:08.519 EST
2023-11-15T20:16:08.519Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : Text: >>>
2023-11-15 15:16:08.519 EST
Cloud Run
2023-11-15 15:16:08.519 EST
2023-11-15T20:16:08.519Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : Text: >>>
2023-11-15 15:16:08.519 EST
2023-11-15T20:16:08.519Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : Text: Cloud
2023-11-15 15:16:08.519 EST
2023-11-15T20:16:08.519Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : Text: Run
2023-11-15 15:16:09.157 EST
2023-11-15T20:16:09.157Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : Result Chat Model: Cloud Run is a serverless compute platform that lets you run stateless containers that are invocable via HTTP requests. It is designed to be fully managed, so you don't need to worry about provisioning or managing servers. You can simply deploy your code
2023-11-15 15:16:09.832 EST
2023-11-15T20:16:09.832Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : Result Text Model: Cloud Run is a serverless compute platform that lets you run stateless containers that are invocable via HTTP requests. It is designed to be fully managed, so you don't need to worry about provisioning or managing servers. You can simply deploy your code
2023-11-15 15:16:09.972 EST
2023-11-15T20:16:09.971Z  INFO 1 --- [nio-8080-exec-5] services.EventController                 : Picture metadata saved in Firestore at 2023-11-15T20:16:09.919019000Z
Show debug panel
```

