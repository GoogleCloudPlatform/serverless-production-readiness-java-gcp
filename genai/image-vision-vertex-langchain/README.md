# Image Analysis - Cloud Run Service using Vision API, Vertex AI and Langchain4j

This lab can be executed directly in a Cloud Workstation, Cloudshell or your environment of choice. 

  [![Open in Cloud Shell](https://gstatic.com/cloudssh/images/open-btn.svg)](https://shell.cloud.google.com/cloudshell/editor?cloudshell_git_repo=https://github.com/GoogleCloudPlatform/serverless-photosharing-workshop.git)

## Setup Java ecosystem
In order to build JIT or Native Java app images, please set up Java and GraalVM and the associated Java 21 distributions.
A simple installer is available from [SDKMan](https://sdkman.io/install)
```shell
sdk install java 21.0.1-graal
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
image-analysis-jit                        latest     6751b98f7ebf   42 years ago    329MB
image-analysis-native                     latest     3af942985d65   42 years ago    262MB
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

Create the bucket:
```shell
# get the Project_ID
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
  or 
export PROJECT_ID=$(gcloud config get-value project)

# Create the GCS bucket
export BUCKET_PICTURES=uploaded-pictures-${PROJECT_ID}
gsutil mb -l EU gs://${BUCKET_PICTURES}
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
gcloud eventarc triggers list --location=eu

gcloud eventarc triggers create image-analysis-jit-trigger \
     --destination-run-service=image-analysis-jit \
     --destination-run-region=us-central1 \
     --location=eu \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=uploaded-pictures-${PROJECT_ID}" \
     --service-account=${PROJECT_NUMBER}-compute@developer.gserviceaccount.com

gcloud eventarc triggers create image-analysis-native-trigger \
     --destination-run-service=image-analysis-native \
     --destination-run-region=us-central1 \
     --location=eu \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=uploaded-pictures-${PROJECT_ID}" \
     --service-account=${PROJECT_NUMBER}-compute@developer.gserviceaccount.com     
```

Test the trigger
```shell
gsutil cp GeekHour.jpeg gs://uploaded-pictures-${PROJECT_ID}
gsutil cp CloudRun.png gs://uploaded-pictures-${PROJECT_ID}

gcloud logging read "resource.labels.service_name=image-analysis-jit AND textPayload:GeekHour" --format=json
```

--------------------
Log capture
```
gcloud logging read "resource.labels.service_name=image-analysis-jit AND textPayload:GeekHour" --format=json

...
 {
    "insertId": "62ebcd66000505e81968501a",
    "labels": {
      "instanceId": "00c527f6d474fb398874cbe473887f91c17dd370c7b10c37f1800a80d7fbfbbbc4ee7eb4fd6149691667dee7cb2f59aae11f65b798fbda01d378a74b8c468c2fe8"
    },
    "logName": "projects/optimize-serverless-apps/logs/run.googleapis.com%2Fstdout",
    "receiveTimestamp": "2022-08-04T13:45:10.528219041Z",
    "resource": {
      "labels": {
        "configuration_name": "image-analysis-jit",
        "location": "us-central1",
        "project_id": "optimize-serverless-apps",
        "revision_name": "image-analysis-jit-00001-waf",
        "service_name": "image-analysis-jit"
      },
      "type": "cloud_run_revision"
    },
    "textPayload": "selfLink : https://www.googleapis.com/storage/v1/b/uploaded-pictures-optimize-serverless-apps/o/GeekHour.jpeg",
    "timestamp": "2022-08-04T13:45:10.329192Z"
  },
  {
    "insertId": "62ebcd66000505dddf95cc89",
    "labels": {
      "instanceId": "00c527f6d474fb398874cbe473887f91c17dd370c7b10c37f1800a80d7fbfbbbc4ee7eb4fd6149691667dee7cb2f59aae11f65b798fbda01d378a74b8c468c2fe8"
    },
    "logName": "projects/optimize-serverless-apps/logs/run.googleapis.com%2Fstdout",
    "receiveTimestamp": "2022-08-04T13:45:10.528219041Z",
    "resource": {
      "labels": {
        "configuration_name": "image-analysis-jit",
        "location": "us-central1",
        "project_id": "optimize-serverless-apps",
        "revision_name": "image-analysis-jit-00001-waf",
        "service_name": "image-analysis-jit"
      },
      "type": "cloud_run_revision"
    },
    "textPayload": "id : uploaded-pictures-optimize-serverless-apps/GeekHour.jpeg/1659620698262814",
    "timestamp": "2022-08-04T13:45:10.329181Z"
  }
...
```

Log capture - Console
```
Default
2022-08-04T13:45:23.090834Zupdated : 2022-08-04T13:44:58.332Z
Default
2022-08-04T13:45:23.090861ZstorageClass : STANDARD
Default
2022-08-04T13:45:23.090869ZtimeStorageClassUpdated : 2022-08-04T13:44:58.332Z
Default
2022-08-04T13:45:23.090926Zsize : 8062
Default
2022-08-04T13:45:23.090942Zmd5Hash : 6Ywof9Kj21ymWv/nwHlwIw==
Default
2022-08-04T13:45:23.090952ZmediaLink : https://www.googleapis.com/download/storage/v1/b/uploaded-pictures-optimize-serverless-apps/o/GeekHour.jpeg?generation=1659620698262814&alt=media
Default
2022-08-04T13:45:23.090973ZcontentLanguage : en
Default
2022-08-04T13:45:23.090990Zcrc32c : l29Spw==
Default
2022-08-04T13:45:23.091147Zetag : CJ6auvGorfkCEAE=
Default
2022-08-04T13:45:23.091158ZDetected change in Cloud Storage bucket: (ce-subject) : objects/GeekHour.jpeg
Default
2022-08-04T13:45:23.091316Z2022-08-04 13:45:23.091 INFO 1 --- [nio-8080-exec-6] services.EventController : New picture uploaded GeekHour.jpeg
Default
2022-08-04T13:45:23.095471Z2022-08-04 13:45:23.095 INFO 1 --- [nio-8080-exec-6] services.EventController : Calling the Vision API...
Info
2022-08-04T13:45:24.137495ZPOST200723 B1.1 sAPIs-Google; (+https://developers.google.com/webmasters/APIs-Google.html) https://image-analysis-jit-6hrfwttbsa-ew.a.run.app/?__GCP_CloudEventsMode=GCS_NOTIFICATION
```

