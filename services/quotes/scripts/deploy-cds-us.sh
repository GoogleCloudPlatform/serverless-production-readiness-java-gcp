# deploy JIT with AppCDS to Cloud Run
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   "Project id: $PROJECT_ID"
echo "Deploy: 2Gi and 2 CPU with AppCDS enabled"

gcloud run deploy quotes-cds \
     --image us-central1-docker.pkg.dev/${PROJECT_ID}/quotes/quotes-cds \
     --region us-central1 \
     --memory 2Gi --cpu=2 \
     --execution-environment gen1 \
     --set-env-vars=SPRING_FLYWAY_ENABLED=false \
     --set-env-vars=JAVA_TOOL_OPTIONS='-XX:+UseG1GC -XX:MaxRAMPercentage=80 -XX:ActiveProcessorCount=2' \
     --cpu-boost \
     --allow-unauthenticated     