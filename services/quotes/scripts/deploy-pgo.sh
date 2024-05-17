# deploy Native PGO to Cloud Run
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   "Project id: $PROJECT_ID"

echo "Deploy: 2Gi and 2 CPU - Native GraalVM Java image with PGO"
gcloud run deploy quotes-pgo \
     --image europe-docker.pkg.dev/${PROJECT_ID}/quotes-native/quotes-pgo \
     --region europe-west1 \
     --memory 2Gi --cpu=2 \
     --execution-environment gen2 \
     --no-cpu-boost \
     --allow-unauthenticated  