gcloud run deploy quotes-native \
     --image europe-docker.pkg.dev/${PROJECT_ID}/quotes-native/quotes-native \
     --region europe-west1 \
     --memory 2Gi --cpu=2 \
     --execution-environment gen2 \
     --set-env-vars=SPRING_FLYWAY_ENABLED=false \
     --no-cpu-boost \
     --allow-unauthenticated  