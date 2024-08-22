export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
export PROJECT_ID=$(gcloud config get-value project)
export PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')

gcloud services enable vision.googleapis.com
gcloud services enable cloudfunctions.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable logging.googleapis.com
gcloud services enable storage-component.googleapis.com
gcloud services enable aiplatform.googleapis.com


export BUCKET_PICTURES=library_next24_images
gsutil mb -l us-central1 gs://${BUCKET_PICTURES}
gsutil uniformbucketlevelaccess set on gs://${BUCKET_PICTURES}
gsutil iam ch allUsers:objectViewer gs://${BUCKET_PICTURES}

export BUCKET_BOOKS_PUBLIC=libarary_next24_public
gsutil mb -l us-central1 gs://${BUCKET_BOOKS_PUBLIC}
gsutil uniformbucketlevelaccess set on gs://${BUCKET_BOOKS_PUBLIC}
gsutil iam ch allUsers:objectViewer gs://${BUCKET_BOOKS_PUBLIC}

export BUCKET_BOOKS_PRIVATE=libarary_next24_private
gsutil mb -l us-central1 gs://${BUCKET_BOOKS_PRIVATE}
gsutil uniformbucketlevelaccess set on gs://${BUCKET_BOOKS_PRIVATE}
gsutil iam ch allUsers:objectViewer gs://${BUCKET_BOOKS_PRIVATE}

gcloud compute addresses create psa-range \
    --global \
    --purpose=VPC_PEERING \
    --prefix-length=24 \
    --description="VPC private service access" \
    --network=default

gcloud services vpc-peerings connect \
    --service=servicenetworking.googleapis.com \
    --ranges=psa-range \
    --network=default

export REGION=us-central1
export ADBCLUSTER=alloydb-aip-01


gcloud alloydb clusters create $ADBCLUSTER \
    --password=$PGPASSWORD \
    --network=default \
    --region=$REGION

gcloud alloydb instances create $ADBCLUSTER-pr \
    --instance-type=PRIMARY \
    --cpu-count=2 \
    --region=$REGION \
    --cluster=$ADBCLUSTER

# create vpc-access connectors
gcloud compute networks vpc-access connectors create alloy-connector \
--region us-central1 \
--network default \
--range 10.100.0.0/28


gcloud run deploy books-genai-native \
    --set-env-vars='MY_PASSWORD=37ni7sSyUEj5Ffb!!!!!5432111NGBIGGUYYANNI,MY_USER=postgres,DB_URL=jdbc:postgresql://172.22.0.2:5432/library,MODEL_ANALYSIS_NAME=text-bison-32k,MODEL_IMAGE_PRO_NAME=text-bison-32k' \
    --image us-docker.pkg.dev/next24-genai-app/books-genai-native/books-genai:latest \
    --region us-central1 \
    --memory 2Gi \
    --cpu 2 \
    --allow-unauthenticated \
    --vpc-connector alloy-connector


gcloud run deploy books-genai-jit \
    --set-env-vars='PROMPT_IMAGE=Extract the book name labels main color and author strictly in JSON format. The json output strictly have property names bookName mainColor author and labels.,MY_PASSWORD=next24-12345!,MY_USER=postgres,DB_URL=jdbc:postgresql://172.22.0.2:5432/library,MODEL_ANALYSIS_NAME=text-bison-32k,MODEL_IMAGE_PRO_NAME=text-bison-32k' \
    --image us-docker.pkg.dev/next24-genai-app/books-genai-jit/books-genai:latest \
    --region us-central1 \
    --memory 2Gi \
    --allow-unauthenticated \
    --vpc-connector alloy-connector

# configure triggers for public and private books, images - accessing the Native Java service image
gcloud eventarc triggers create books-genai-jit-trigger-image \
     --destination-run-service=books-genai-jit \
     --destination-run-region=us-central1 \
     --destination-run-path=/images \
     --location=us-central1 \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=vision-${PROJECT_ID}" \
     --service-account=${PROJECT_NUMBER}-compute@developer.gserviceaccount.com

gcloud eventarc triggers create books-genai-native-trigger-image \
     --destination-run-service=books-genai-native \
     --destination-run-region=us-central1 \
     --destination-run-path=/images \
     --location=us-central1 \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=vision-${PROJECT_ID}" \
     --service-account=${PROJECT_NUMBER}-compute@developer.gserviceaccount.com

gcloud eventarc triggers create books-genai-jit-trigger-embeddings \
     --destination-run-service=books-genai-jit \
     --destination-run-region=us-central1 \
     --destination-run-path=/document/embeddings \
     --location=us-central1 \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=books-${PROJECT_ID}" \
     --service-account=48099017975-compute@developer.gserviceaccount.com

# configure triggers for public and private books, images - accessing the Native Java service image
gcloud eventarc triggers create books-genai-native-trigger-public \
     --destination-run-service=books-genai-native \
     --destination-run-region=us-central1 \
     --destination-run-path=/document/embeddings \
     --location=us-central1 \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=library_next24_public" \
     --service-account=48099017975-compute@developer.gserviceaccount.com

gcloud eventarc triggers create books-genai-native-trigger-private \
     --destination-run-service=books-genai-native \
     --destination-run-region=us-central1 \
     --destination-run-path=/document/embeddings \
     --location=us-central1 \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=library_next24_private" \
     --service-account=48099017975-compute@developer.gserviceaccount.com

gcloud eventarc triggers create books-genai-native-trigger-image \
     --destination-run-service=books-genai-native \
     --destination-run-region=us-central1 \
     --destination-run-path=/images \
     --location=us-central1 \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=library_next24_images" \
     --service-account=48099017975-compute@developer.gserviceaccount.com
