#configure org policies, service accounts with permissions

cat <<EOF > policy.yaml
constraint: constraints/iam.allowedPolicyMemberDomains
listPolicy:
  allValues: ALLOW
EOF

gcloud resource-manager org-policies set-policy policy.yaml --organization=419713829424
export SERVICE_ACCOUNT=48099017975-compute@developer.gserviceaccount.com
gcloud eventarc triggers list --location=us-central1
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