## Setup permissions (*prior to running tf)

```shell
export PROJECT_ID=$(gcloud config get-value project)
export PROJECT_NUMBER=$(gcloud projects list --filter="$(gcloud config get-value project)" --format="value(PROJECT_NUMBER)")
export SERVICE_ACCOUNT=${PROJECT_NUMBER}-compute@developer.gserviceaccount.com

gcloud projects add-iam-policy-binding ${PROJECT_ID}     --member="serviceAccount:${SERVICE_ACCOUNT}"     --role='roles/storage.objectViewer'

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:service-${PROJECT_NUMBER}@gcp-sa-eventarc.iam.gserviceaccount.com" \
    --role="roles/eventarc.serviceAgent"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:service-${PROJECT_NUMBER}@gcp-sa-eventarc.iam.gserviceaccount.com" \
    --role="roles/run.invoker"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
    --role="roles/eventarc.eventReceiver"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
--member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
--role="roles/aiplatform.user"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
--member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
--role="roles/datastore.user" 

gcloud projects add-iam-policy-binding $PROJECT_ID \
--member="serviceAccount:service-${PROJECT_NUMBER}@gcp-sa-alloydb.iam.gserviceaccount.com" \
--role="roles/aiplatform.user"

gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:service-${PROJECT_NUMBER}@gs-project-accounts.iam.gserviceaccount.com" \
    --role='roles/pubsub.publisher' 

export ORG_ID=$(gcloud organizations list --format="value(name)")
cat <<EOF > policy.yaml
constraint: constraints/compute.vmExternalIpAccess
listPolicy:
  allValues: ALLOW
EOF
gcloud resource-manager org-policies set-policy policy.yaml --organization=${ORG_ID}
```
## Edit terraform.tfvars file populate my_user, alloydb_cluster_name, project_number, & project_id before running TF

```shell
terraform init 
terraform plan
terraform apply
#import state if needed e.g. vpc that already exists:
terraform import google_compute_network.auto_vpc projects/books-genai-tf-test/global/networks/default
```

## Terraform must be run twice. The first run creates the infrastructure such as artifact registry, VPC, firewall, VPC peering, GCE psql client, firestore, & alloydb.
## After the first time you run TF & the above infra is created. Tag and push the cloud run images to the artifact registry you just created.
```shell
docker push us-central1-docker.pkg.dev/books-genai-tf-test/books-genai-jit/books-genai:latest
docker push us-central1-docker.pkg.dev/books-genai-tf-test/books-genai-native/books-genai:latest
```
## Once the images are there then the cloud run instances will be ready to deploy on the second terraform apply.. (First run will fail here)

## Issues & TODOs
## CI to build the images and push to artifact registry..GCE instance need external ip to install psql. 403 error for unauthenticated invocations

```json
//2024-04-04 11:34:04.320 PDT
//POST4030 B0 msAPIs-Google; (+https://developers.google.com/webmasters/APIs-Google.html) https://books-genai-jit-6cjxsratvq-uc.a.run.app/document/embeddings?__GCP_CloudEventsMode=GCS_NOTIFICATION
{
httpRequest: {9}
insertId: "660ef29c0006076a79dfec8d"
logName: "projects/books-genai-tf-test/logs/run.googleapis.com%2Frequests"
receiveTimestamp: "2024-04-04T18:34:04.400277423Z"
resource: {2}
severity: "WARNING"
spanId: "1021680556677630213"
textPayload: "The request was not authenticated. Either allow unauthenticated invocations or set the proper Authorization header. Read more at https://cloud.google.com/run/docs/securing/authenticating Additional troubleshooting documentation can be found at: https://cloud.google.com/run/docs/troubleshooting#unauthorized-client"
timestamp: "2024-04-04T18:34:04.320020Z"
trace: "projects/books-genai-tf-test/traces/32c73e42c7ed3915a001e466d83da3d4"
traceSampled: true
}
```