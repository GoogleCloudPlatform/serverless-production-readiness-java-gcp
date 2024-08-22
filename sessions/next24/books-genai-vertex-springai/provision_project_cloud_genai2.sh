#configure org policies, service accounts with permissions
# setup alloy db follow these steps on codelabs https://codelabs.developers.google.com/codelabs/alloydb-ai-embedding#4

cat <<EOF > policy.yaml
constraint: constraints/iam.allowedPolicyMemberDomains
listPolicy:
  allValues: ALLOW
EOF

gcloud resource-manager org-policies set-policy policy.yaml --organization=419713829424
export SERVICE_ACCOUNT=48099017975-compute@developer.gserviceaccount.com
gcloud eventarc triggers list --location=us-central1

# configure triggers for public and private books, images - accessing the JIT service image
gcloud eventarc triggers create books-genai-jit-trigger-public \
     --destination-run-service=books-genai-jit \
     --destination-run-region=us-central1 \
     --destination-run-path=/document/embeddings \
     --location=us-central1 \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=library_next24_public" \
     --service-account=48099017975-compute@developer.gserviceaccount.com

gcloud eventarc triggers create books-genai-jit-trigger-private \
     --destination-run-service=books-genai-jit \
     --destination-run-region=us-central1 \
     --destination-run-path=/document/embeddings \
     --location=us-central1 \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=library_next24_private" \
     --service-account=48099017975-compute@developer.gserviceaccount.com

gcloud eventarc triggers create books-genai-jit-trigger-image \
     --destination-run-service=books-genai-jit \
     --destination-run-region=us-central1 \
     --destination-run-path=/images \
     --location=us-central1 \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=library_next24_images" \
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

gcloud projects add-iam-policy-binding next24-genai-app \
    --member="serviceAccount:service-48099017975@gcp-sa-eventarc.iam.gserviceaccount.com" \
    --role="roles/eventarc.serviceAgent"

gcloud projects add-iam-policy-binding next24-genai-app \
    --member="serviceAccount:service-48099017975@gcp-sa-eventarc.iam.gserviceaccount.com" \
    --role="roles/run.invoker"

gcloud projects add-iam-policy-binding next24-genai-app \
    --member="serviceAccount:48099017975-compute@developer.gserviceaccount.com" \
    --role="roles/eventarc.eventReceiver"
gcloud projects add-iam-policy-binding next24-genai-app \
  --member="serviceAccount:48099017975-compute@developer.gserviceaccount.com" \
  --role="roles/aiplatform.user"
gcloud projects add-iam-policy-binding next24-genai-app \
  --member="serviceAccount:48099017975-compute@developer.gserviceaccount.com" \
  --role="roles/datastore.user"
gcloud firestore databases create --region=us-central1;

gcloud projects add-iam-policy-binding ${PROJECT_ID}     --member="serviceAccount:${SERVICE_ACCOUNT}"     --role='roles/storage.objectViewer'

#Deployment steps:
git pull
rm -rf target/
docker rmi books-genai-jit:latest
docker rmi us-docker.pkg.dev/next24-genai-app/books-genai-jit/books-genai:latest
./mvnw package -Dmaven.test.skip
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=books-genai-jit -Dmaven.test.skip
docker tag books-genai-jit:latest us-docker.pkg.dev/next24-genai-app/books-genai-jit/books-genai:latest
gcloud artifacts docker images delete "us-docker.pkg.dev/next24-genai-app/books-genai-jit/books-genai:latest" --project=next24-genai-app
docker push us-docker.pkg.dev/next24-genai-app/books-genai-jit/books-genai:latest
gcloud compute networks vpc-access connectors create alloy-connector \
  --region us-central1 \
  --network default \
  --range 10.100.0.0/28  # Optional if you have a free /28 subnet
gcloud run deploy books-genai-jit \
    --set-env-vars='MY_PASSWORD=pword,MY_USER=pguser,DB_URL=jdbc:postgresql://0.0.0.0:8000/db,MODEL_ANALYSIS_NAME=text-bison-32k,MODEL_IMAGE_PRO_NAME=text-bison-32k' \
    --image us-docker.pkg.dev/next24-genai-app/books-genai-jit/books-genai:latest \
    --region us-central1 \
    --memory 2Gi \
    --allow-unauthenticated \
    --vpc-connector alloy-connector
gcloud run deploy books-genai-native \
    --set-env-vars='MY_PASSWORD=pword,MY_USER=pguser,DB_URL=jdbc:postgresql://0.0.0.0:8000/db,MODEL_ANALYSIS_NAME=text-bison-32k,MODEL_IMAGE_PRO_NAME=text-bison-32k' \
    --image us-docker.pkg.dev/next24-genai-app/books-genai-native/books-genai:latest \
    --region us-central1 \
    --memory 2Gi \
    --cpu 2 \
    --allow-unauthenticated \
    --vpc-connector alloy-connector


#sample bash script
export PROJECT_ID=$(gcloud config get-value project)
export REGION=us-central1
export PROJECT_NUMBER=$(gcloud projects list --filter="$(gcloud config get-value project)" --format="value(PROJECT_NUMBER)")
export SERVICE_ACCOUNT=${PROJECT_NUMBER}-compute@developer.gserviceaccount.com

gcloud services enable vision.googleapis.com
gcloud services enable cloudfunctions.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable logging.googleapis.com
gcloud services enable storage-component.googleapis.com
gcloud services enable aiplatform.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable alloydb.googleapis.com
gcloud services enable artifactregistry.googleapis.com
gcloud services enable vpcaccess.googleapis.com
gcloud services enable servicenetworking.googleapis.com
gcloud services enable eventarc.googleapis.com
gcloud services enable firestore.googleapis.com

gcloud projects add-iam-policy-binding ${PROJECT_ID}     --member="serviceAccount:${SERVICE_ACCOUNT}"     --role='roles/storage.objectViewer'

export SERVICE_ACCOUNT_KMS="$(gsutil kms serviceaccount -p ${PROJECT_ID})"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT_KMS}" \
    --role='roles/pubsub.publisher'

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
--member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
--role="roles/aiplatform.user"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
--member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
--role="roles/datastore.user"

#This resolves the error when eventarc publishes msg to cloudrun: "Error: The request was not authenticated. Either allow unauthenticated invocations or set the proper Authorization header"
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
--member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
--role="roles/run.invoker"

gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:@gs-project-accounts.iam.gserviceaccount.com">service-${PROJECT_NUMBER}@gs-project-accounts.iam.gserviceaccount.com" \
    --role='roles/pubsub.publisher'

#Apply after the first time you run the terraform for alloy.. Or else you will get an error this service account doesn't exist
gcloud projects add-iam-policy-binding $PROJECT_ID \
--member="serviceAccount:@gcp-sa-alloydb.iam.gserviceaccount.com">service-${PROJECT_NUMBER}@gcp-sa-alloydb.iam.gserviceaccount.com" \
--role="roles/aiplatform.user"

#Apply eventarc permissions when you see in tf Error 400: Invalid resource state for "": Permission denied while using the Eventarc Service Agent.
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:@gcp-sa-eventarc.iam.gserviceaccount.com">service-${PROJECT_NUMBER}@gcp-sa-eventarc.iam.gserviceaccount.com" \
    --role="roles/eventarc.serviceAgent"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:@gcp-sa-eventarc.iam.gserviceaccount.com">service-${PROJECT_NUMBER}@gcp-sa-eventarc.iam.gserviceaccount.com" \
    --role="roles/run.invoker"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
    --role="roles/eventarc.eventReceiver"

# configure triggers for public and private books, images - accessing the JIT service image
gcloud eventarc triggers create rag-genai-jit-trigger-public \
     --destination-run-service=rag-genai-jit \
     --destination-run-region=us-central1 \
     --destination-run-path=/document/embeddings \
     --location=us-central1 \
     --event-filters="type=google.cloud.storage.object.v1.finalized" \
     --event-filters="bucket=bucket" \
     --service-account="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

gcloud compute addresses create psa-range \
    --global \
    --purpose=VPC_PEERING \
    --prefix-length=24 \
    --description="VPC private service access" \
    --network=default

# Create private connection using the allocated IP range:

gcloud services vpc-peerings connect \
    --service=servicenetworking.googleapis.com \
    --ranges=psa-range \
    --network=default

gcloud compute networks vpc-access connectors create alloy-connector \
  --region us-central1 \
  --network default \
  --range 10.100.0.0/28

export PGPASSWORD=`openssl rand -hex 12`

gcloud firestore databases create $ADBCLUSTER \
 --region=us-central \
 --network=default \
 --password=$PGPASSWORD

gcloud alloydb instances create $ADBCLUSTER-pr \
    --instance-type=PRIMARY \
    --cpu-count=2 \
    --region=$REGION \
    --cluster=$ADBCLUSTER

gcloud run deploy document-genai-jit \
    --set-env-vars='MY_PASSWORD=pword,MY_USER=pguser,DB_URL=jdbc:postgresql://host-ip:5432/db,MODEL_ANALYSIS_NAME=gemini-1.5-flash-001' \
    --image us-docker.pkg.dev/${PROJECT_ID}/your-repo/rag-genai-app:latest \
    --region us-central1 \
    --memory 2Gi \
    --allow-unauthenticated \
    --vpc-connector alloy-connector