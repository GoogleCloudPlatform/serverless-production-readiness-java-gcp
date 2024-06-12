export VERTEX_AI_GEMINI_MODEL=gemini-1.5-flash-001
export VERTEX_AI_GEMINI_LOCATION=us-central1
export VERTEX_AI_GEMINI_PROJECT_ID=next24-genai-app


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

gcloud compute addresses create psa-range \
    --global \
    --purpose=VPC_PEERING \
    --prefix-length=24 \
    --description="VPC private service access" \
    --network=default
