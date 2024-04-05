## Setup permissions (*prior to running tf)

```shell
export PROJECT_ID=$(gcloud config get-value project)
export REGION=us-central1
export PROJECT_NUMBER=$(gcloud projects list --filter="$(gcloud config get-value project)" --format="value(PROJECT_NUMBER)")
export SERVICE_ACCOUNT=${PROJECT_NUMBER}-compute@developer.gserviceaccount.com

gcloud services enable vision.googleapis.com
gcloud services enable cloudfunctions.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable logging.googleapis.com 
gcloud services enable storage-component.googleapis.com 
gcloud services enable aiplatform.googleapis.com
gcloud services enable run.googleapis.com 
gcloud services enable alloydb.googleapis.com 
gcloud services enable artifactregistry.googleapis.com
gcloud services enable vpcaccess.googleapis.com
gcloud services enable servicenetworking.googleapis.com
gcloud services enable eventarc.googleapis.com
gcloud services enable firestore.googleapis.com

gcloud projects add-iam-policy-binding ${PROJECT_ID}     --member="serviceAccount:${SERVICE_ACCOUNT}"     --role='roles/storage.objectViewer'

export SERVICE_ACCOUNT_KMS="$(gsutil kms serviceaccount -p ${PROJECT_ID})"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${SERVICE_ACCOUNT_KMS}" \
    --role='roles/pubsub.publisher'

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
--member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
--role="roles/aiplatform.user"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
--member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
--role="roles/datastore.user" 

#This resolves the error when eventarc publishes msg to cloudrun: "Error: The request was not authenticated. Either allow unauthenticated invocations or set the proper Authorization header"
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
--member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
--role="roles/run.invoker"  

gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:service-${PROJECT_NUMBER}@gs-project-accounts.iam.gserviceaccount.com" \
    --role='roles/pubsub.publisher' 

#Apply after the first time you run the terraform for alloy.. Or else you will get an error this service account doesn't exist
gcloud projects add-iam-policy-binding $PROJECT_ID \
--member="serviceAccount:service-${PROJECT_NUMBER}@gcp-sa-alloydb.iam.gserviceaccount.com" \
--role="roles/aiplatform.user"

#Apply eventarc permissions when you see in tf Error 400: Invalid resource state for "": Permission denied while using the Eventarc Service Agent. 
gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:service-${PROJECT_NUMBER}@gcp-sa-eventarc.iam.gserviceaccount.com" \
    --role="roles/eventarc.serviceAgent"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:service-${PROJECT_NUMBER}@gcp-sa-eventarc.iam.gserviceaccount.com" \
    --role="roles/run.invoker"

gcloud projects add-iam-policy-binding ${PROJECT_ID} \
    --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
    --role="roles/eventarc.eventReceiver"

#These two org policies are needed for VM to get external ip & Cloud Run to allow unauthenticated
export ORG_ID=$(gcloud organizations list --format="value(name)")
cat <<EOF > policy.yaml
constraint: constraints/compute.vmExternalIpAccess
listPolicy:
  allValues: ALLOW
EOF
gcloud resource-manager org-policies set-policy policy.yaml --organization=${ORG_ID}
cat <<EOF > policy.yaml
constraint: constraints/iam.allowedPolicyMemberDomains
listPolicy:
  allValues: ALLOW
EOF
gcloud resource-manager org-policies set-policy policy.yaml --organization=${ORG_ID}
```
## Edit terraform.tfvars file populate my_user, alloydb_cluster_name, project_number, & project_id before running TF

```shell
terraform init 
terraform plan
#Password will be prompted. This is a password that you setup on the first run.
terraform apply
```

## Terraform must be run twice. The first run creates the infrastructure such as artifact registry, VPC, firewall, VPC peering, GCE psql client, firestore, & alloydb.
## After the first time you run TF & the above infra is created. Tag and push the cloud run images to the artifact registry you just created. Example below:
```shell
docker push ${REGION}-docker.pkg.dev/${PROJECT_ID}/books-genai-jit/books-genai:latest
docker push ${REGION}-docker.pkg.dev/${PROJECT_ID}/books-genai-native/books-genai:latest
```
## Once the images are there then the cloud run instances will be ready to deploy on the second terraform apply.. (First run will fail here)

## Issues & TODOs
## CI to build the images and push to artifact registry..GCE instance need external ip to install psql & download sql with ddl. 
## Cloud run need to allow unauthenticated