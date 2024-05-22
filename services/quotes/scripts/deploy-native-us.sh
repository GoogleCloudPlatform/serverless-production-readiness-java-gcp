# deploy Native to Cloud Run
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   "Project id: $PROJECT_ID"

echo "Deploy: 2Gi and 2 CPU - Native GraalVM Java image"
gcloud run deploy quotes-native \
     --image us-central1-docker.pkg.dev/${PROJECT_ID}/quotes-native/quotes-native \
     --region us-central1 \
     --memory 2Gi --cpu=2 \
     --execution-environment gen1 \
     --set-env-vars=SPRING_FLYWAY_ENABLED=false \
     --no-cpu-boost \
     --allow-unauthenticated  