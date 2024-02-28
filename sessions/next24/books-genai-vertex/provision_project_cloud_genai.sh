# turn proxy instance on for alloy db and change security for cloud run Allow unauthenticated invocations on UI
#constraint: constraints/iam.allowedPolicyMemberDomains
#listPolicy:
# allValues: ALLOW

gcloud resource-manager org-policies set-policy policy.yaml --organization=419713829424

export SERVICE_ACCOUNT=48099017975-compute@developer.gserviceaccount.com

gcloud run deploy books-genai-jit      --image us-docker.pkg.dev/next24-genai-app/books-genai-jit/books-genai:latest --region us-central1      --memory 2Gi --allow-unauthenticated

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
