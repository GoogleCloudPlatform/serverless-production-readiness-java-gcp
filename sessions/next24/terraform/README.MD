## Setup permissions (*prior to running tf)

```shell
export PROJECT_ID=$(gcloud config get-value project)
export REGION=us-central1
export PROJECT_NUMBER=$(gcloud projects list --filter="$(gcloud config get-value project)" --format="value(PROJECT_NUMBER)")
export SERVICE_ACCOUNT=${PROJECT_NUMBER}-compute@developer.gserviceaccount.com

gcloud projects add-iam-policy-binding ${PROJECT_ID}     --member="serviceAccount:${SERVICE_ACCOUNT}"     --role='roles/storage.objectViewer'

export SERVICE_ACCOUNT_KMS="$(gsutil kms serviceaccount -p ${PROJECT_ID})"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT_KMS}" \
    --role='roles/pubsub.publisher'

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
```

## Terraform must be run twice. The first run creates the infrastructure such as artifact registry, VPC, firewall, VPC peering, GCE psql client, firestore, & alloydb.
## After the first time you run TF & the above infra is created. Tag and push the cloud run images to the artifact registry you just created. Example below:
```shell
docker push ${REGION}docker.pkg.dev/${PROJECT_ID}/books-genai-jit/books-genai:latest
docker push ${REGION}-docker.pkg.dev/${PROJECT_ID}/books-genai-native/books-genai:latest
```
## Once the images are there then the cloud run instances will be ready to deploy on the second terraform apply.. (First run will fail here)

## Issues & TODOs
## CI to build the images and push to artifact registry..GCE instance need external ip to install psql & download sql with ddl