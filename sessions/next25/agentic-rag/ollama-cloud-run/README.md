### Steps to Build and deploy Ollama Container

1.  **Update Configuration Files:**

    -   Edit `cloudbuild.yaml` to include your Google Cloud project name.
    -   Edit `Dockerfile` with the list of models you wish to
        pre-download.

2.  **Build the Container:** `gcloud builds submit --region=us-central1 --config cloudbuild.yaml`

3.  **Deploy the Container:** You can deploy the Ollama container
    using the example service.yaml file. This is not needed if you are
    planning to deploy the UI in part 2.

    -   Update `cloudbuild.yaml` with the Google Cloud project ID
    -   Deploy on Cloud Run: `gcloud alpha run services replace service.yaml`
