# Image Analysis - Cloud Run Service using Vision API, Vertex AI and Langchain4j

This lab can be executed directly in a Cloud Workstation, Cloudshell or your environment of choice. 

  [![Open in Cloud Shell](https://gstatic.com/cloudssh/images/open-btn.svg)](https://shell.cloud.google.com/cloudshell/editor?cloudshell_git_repo=https://github.com/GoogleCloudPlatform/serverless-photosharing-workshop.git)

## Setup Java ecosystem
In order to build JIT or Native Java app images, please set up Java and GraalVM and the associated Java 21 distributions.
A simple installer is available from [SDKMan](https://sdkman.io/install)
```shell
sdk install java 21.0.4-graal
```

## Clone the code:
```shell
git clone https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp.git
cd genai/image-vision-vertex-langchain/
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

Build Native Java Tests:
```shell
./mvnw clean package -Pnative,nativeTest

Run the native tests locally:
./mvnw native-tests
```
### Build a JIT and Native Java container Image
```shell
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=image-analysis-jit

./mvnw spring-boot:build-image  -DskipTests -Pnative -Dspring-boot.build-image.imageName=image-analysis-native
```

Check the Docker image sizes:
```shell
docker images | grep image-analysis
image-analysis-native                                     latest                aa0f7b406966   44 years ago    372MB
image-analysis-jit                                        latest                67b3489cbec8   44 years ago    467MB
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
 gcloud logging read "resource.labels.service_name=image-analysis-native AND textPayload:CloudRun" --format=json
```


