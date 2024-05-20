# deploy JIT with AppCDS to Cloud Run
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   "Project id: $PROJECT_ID"
echo "Deploy: 2Gi and 2 CPU with Project Leyden Premain"

gcloud run deploy quotes-leyden-premain \
     --image europe-docker.pkg.dev/${PROJECT_ID}/quotes/quotes-leyden-premain \
     --region europe-west1 \
     --memory 2Gi --cpu=2 \
     --execution-environment gen2 \
     --set-env-vars=SPRING_FLYWAY_ENABLED=false \
     --set-env-vars=JAVA_TOOL_OPTIONS='-XX:+UseG1GC -XX:MaxRAMPercentage=80 -XX:ActiveProcessorCount=2' \
     --cpu-boost \
     --allow-unauthenticated
