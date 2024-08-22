# Configure the GCP provider
provider "google" {
project = var.project_id
region  = var.region
}

# Create the storage buckets
resource "google_storage_bucket" "bucket_pictures" {
name          = var.bucket_pictures
location      = "US-CENTRAL1"
force_destroy = true
uniform_bucket_level_access = true

storage_class = "STANDARD"
}

resource "google_storage_bucket_iam_member" "bucket_pictures_viewer" {
bucket = google_storage_bucket.bucket_pictures.name
role   = "roles/storage.objectViewer"
member = "allUsers"
}

resource "google_storage_bucket" "bucket_books_public" {
name          = var.bucket_books_public
location      = "US-CENTRAL1"
force_destroy = true
uniform_bucket_level_access = true

storage_class = "STANDARD"
}

resource "google_storage_bucket_iam_member" "bucket_books_public_viewer" {
bucket = google_storage_bucket.bucket_books_public.name
role   = "roles/storage.objectViewer"
member = "allUsers"
}

resource "google_storage_bucket" "bucket_books_private" {
name          = var.bucket_books_private
location      = "US-CENTRAL1"
force_destroy = true
uniform_bucket_level_access = true

storage_class = "STANDARD"
}

resource "google_storage_bucket_iam_member" "bucket_books_private_viewer" {
bucket = google_storage_bucket.bucket_books_private.name
role   = "roles/storage.objectViewer"
member = "allUsers"
}

# Create the global address for VPC peering
resource "google_compute_address" "psa_range" {
name         = "psa-range"
address_type = "EXTERNAL"
purpose      = "VPC_PEERING"
prefix_length = 24
description  = "VPC private service access"
network      = "default"
}

# Connect to the Service Networking API
resource "google_service_networking_connection" "servicenetworking_connection" {
service = "servicenetworking.googleapis.com"
network = "default"
reserved_peering_ranges = [
google_compute_address.psa_range.name,
]
}

# Enable required APIs
resource "google_service_account" "service_account" {
account_id   = "compute-${google_project_service.project.number}"
display_name = "Compute Service Account"
}

resource "google_project_service" "project" {
service = "vision.googleapis.com"
disable_on_destroy = false
}

resource "google_project_service" "project_1" {
service = "cloudfunctions.googleapis.com"
disable_on_destroy = false
}

resource "google_project_service" "project_2" {
service = "cloudbuild.googleapis.com"
disable_on_destroy = false
}

resource "google_project_service" "project_3" {
service = "run.googleapis.com"
disable_on_destroy = false
}

resource "google_project_service" "project_4" {
service = "logging.googleapis.com"
disable_on_destroy = false
}

resource "google_project_service" "project_5" {
service = "storage-component.googleapis.com"
disable_on_destroy = false
}

resource "google_project_service" "project_6" {
service = "aiplatform.googleapis.com"
disable_on_destroy = false
}

# Create the AlloyDB cluster
resource "google_alloydb_cluster" "alloydb_cluster" {
name     = var.adbcluster
region   = var.region
network  = "default"
password = var.pgpassword
timeouts {
create = "30m"
delete = "30m"
update = "30m"
}
}

# Create the AlloyDB instance
resource "google_alloydb_instance" "alloydb_instance" {
name         = "${var.adbcluster}-pr"
instance_type = "PRIMARY"
cpu_count    = 2
region       = var.region
cluster      = google_alloydb_cluster.alloydb_cluster.name
timeouts {
create = "30m"
delete = "30m"
update = "30m"
}
}

# Create the VPC Access connector
resource "google_compute_network_vpc_access_connector" "alloy_connector" {
name     = "alloy-connector"
network  = "default"
region   = var.region
range    = "10.100.0.0/28"
timeouts {
create = "30m"
delete = "30m"
update = "30m"
}
}

# Deploy the Cloud Run services
resource "google_cloud_run_v2_service" "books_genai_native" {
name     = "books-genai-native"
location = var.region
template {
containers {
image = "us-docker.pkg.dev/next24-genai-app/books-genai-native/books-genai:latest"
resources {
limits {
cpu    = 2
memory = "2Gi"
}
}
env {
name  = "MY_PASSWORD"
value = var.my_password
}
env {
name  = "MY_USER"
value = var.my_user
}
env {
name  = "DB_URL"
value = var.db_url
}
env {
name  = "MODEL_ANALYSIS_NAME"
value = var.model_analysis_name
}
env {
name  = "MODEL_IMAGE_PRO_NAME"
value = var.model_image_pro_name
}
}
timeout_seconds = 120
}
traffic {
percent = 100
latest_revision = true
}
autogenerate_revision_name = true
metadata {
annotations = {
"run.googleapis.com/ingress" = "all"
}
}
vpc_access {
connector = google_compute_network_vpc_access_connector.alloy_connector.name
}
timeouts {
create = "30m"
delete = "30m"
update = "30m"
}
}

resource "google_cloud_run_v2_service" "books_genai_jit" {
name     = "books-genai-jit"
location = var.region
template {
containers {
image = "us-docker.pkg.dev/next24-genai-app/books-genai-jit/books-genai:latest"
resources {
limits {
cpu    = 2
memory = "2Gi"
}
}
env {
name  = "PROMPT_IMAGE"
value = var.prompt_image
}
env {
name  = "MY_PASSWORD"
value = var.my_password
}
env {
name  = "MY_USER"
value = var.my_user
}
env {
name  = "DB_URL"
value = var.db_url
}
env {
name  = "MODEL_ANALYSIS_NAME"
value = var.model_analysis_name
}
env {
name  = "MODEL_IMAGE_PRO_NAME"
value = var.model_image_pro_name
}
}
timeout_seconds = 120
}
traffic {
percent = 100
latest_revision = true
}
autogenerate_revision_name = true
metadata {
annotations = {
"run.googleapis.com/ingress" = "all"
}
}
vpc_access {
connector = google_compute_network_vpc_access_connector.alloy_connector.name
}
timeouts {
create = "30m"
delete = "30m"
update = "30m"
}
}

# Create Eventarc triggers
resource "google_eventarc_trigger" "books_genai_jit_trigger_image" {
name     = "books-genai-jit-trigger-image"
location = var.region
event_filters {
type = "google.cloud.storage.object.v1.finalized"
attribute {
key   = "bucket"
value = "vision-${google_project_service.project.number}"
}
}
destination {
cloud_run {
service = google_cloud_run_v2_service.books_genai_jit.name
path     = "/images"
region   = var.region
}
}
service_account = google_service_account.service_account.email
timeouts {
create = "30m"
delete = "30m"
update = "30m"
}
}

resource "google_eventarc_trigger" "books_genai_native_trigger_image" {
name     = "books-genai-native-trigger-image"
location = var.region
event_filters {
type = "google.cloud.storage.object.v1.finalized"
attribute {
key   = "bucket"
value = "vision-${google_project_service.project.number}"
}
}
destination {
cloud_run {
service = google_cloud_run_v2_service.books_genai_native.name
path     = "/images"
region   = var.region
}
}
service_account = google_service_account.service_account.email
timeouts {
create = "30m"
delete = "30m"
update = "30m"
}
}

resource "google_eventarc_trigger" "books_genai_jit_trigger_embeddings" {
name     = "books-genai-jit-trigger-embeddings"
location = var.region
event_filters {
type = "google.cloud.storage.object.v1.finalized"
attribute {
key   = "bucket"
value = "books-${google_project_service.project.number}"
}
}
destination {
cloud_run {
service = google_cloud_run_v2_service.books_genai_jit.name
path     = "/document/embeddings"
region   = var.region
}
}
service_account = google_service_account.service_account.email
timeouts {
create = "30m"
delete = "30m"
update = "30m"
}
}

resource "google_eventarc_trigger" "books_genai_native_trigger_public" {
name     = "books-genai-native-trigger-public"
location = var.region
event_filters {
type = "google.cloud.storage.object.v1.finalized"
attribute {
key   = "bucket"
value = var.bucket_books_public
}
}
destination {
cloud_run {
service = google_cloud_run_v2_service.books_genai_native.name
path     = "/document/embeddings"
region   = var.region
}
}
service_account = google_service_account.service_account.email
timeouts {
create = "30m"
delete = "30m"
update = "30m"
}
}

resource "google_eventarc_trigger" "books_genai_native_trigger_private" {
name     = "books-genai-native-trigger-private"
location = var.region
event_filters {
type = "google.cloud.storage.object.v1.finalized"
attribute {
key   = "bucket"
value = var.bucket_books_private
}
}
destination {
cloud_run {
service = google_cloud_run_v2_service.books_genai_native.name
path     = "/document/embeddings"
region   = var.region
}
}
service_account = google_service_account.service_account.email
timeouts {
create = "30m"
delete = "30m"
update = "30m"
}
}

resource "google_eventarc_trigger" "books_genai_native_trigger_image_1" {
name     = "books-genai-native-trigger-image"
location = var.region
event_filters {
type = "google.cloud.storage.object.v1.finalized"
attribute {
key   = "bucket"
value = var.bucket_pictures
}
}
destination {
cloud_run {
service = google_cloud_run_v2_service.books_genai_native.name
path     = "/images"
region   = var.region
}
}
service_account = google_service_account.service_account.email
timeouts {
create = "30m"
delete = "30m"
update = "30m"
}
}

variable "project_id" {
type = string
description = "The Google Cloud Project ID"
}

variable "region" {
type = string
description = "The GCP region for the resources"
default = "us-central1"
}

variable "bucket_pictures" {
type = string
description = "The name of the bucket for images"
default = "library_next24_images"
}

variable "bucket_books_public" {
type = string
description = "The name of the bucket for public books"
default = "libarary_next24_public"
}

variable "bucket_books_private" {
type = string
description = "The name of the bucket for private books"
default = "libarary_next24_private"
}

variable "adbcluster" {
type = string
description = "The name of the AlloyDB cluster"
default = "alloydb-aip-01"
}

variable "pgpassword" {
type = string
description = "The password for the AlloyDB cluster"
sensitive = true
}

variable "my_password" {
type = string
description = "The password for the database connection"
sensitive = true
}

variable "my_user" {
type = string
description = "The username for the database connection"
}

variable "db_url" {
type = string
description = "The database connection URL"
}

variable "model_analysis_name" {
type = string
description = "The name of the model for analysis"
default = "text-bison-32k"
}

variable "model_image_pro_name" {
type = string
description = "The name of the model for image processing"
default = "text-bison-32k"
}

variable "prompt_image" {
type = string
description = "The prompt for image analysis"
default = "Extract the book name labels main color and author strictly in JSON format. The json output strictly have property names bookName mainColor author and labels."
}
#
#**2. Explanation:**
#
#* **`main.tf`:**
#* **Provider Configuration:** Defines the GCP provider with the project ID and region.
#* **Storage Buckets:** Creates three storage buckets (`library_next24_images`, `libarary_next24_public`, `libarary_next24_private`) with uniform bucket-level access and grants all users object viewer permissions.
#* **Global Address:** Creates a global address for VPC peering with the specified parameters.
#* **Service Networking Connection:** Connects to the Service Networking API using the created global address.
#* **API Enablement:** Enables the necessary Google Cloud APIs (Vision, Cloud Functions, Cloud Build, Cloud Run, Logging, Storage, AI Platform).
#* **AlloyDB Cluster and Instance:** Creates an AlloyDB cluster and a primary instance within the specified region and network.
#* **VPC Access Connector:** Creates a VPC Access connector for connecting to the AlloyDB cluster.
#* **Cloud Run Services:** Deploys two Cloud Run services (`books-genai-native` and `books-genai-jit`) with specified environment variables, image, memory, CPU, and VPC access connector.
#* **Eventarc Triggers:** Creates Eventarc triggers for both services, listening for events from the storage buckets and invoking the corresponding Cloud Run service.
#* **`variables.tf`:**
#* **Input Variables:** Declares all input variables used in `main.tf`, including project ID, region, bucket names, database credentials, model names, and other configuration parameters.
#
#**3. Best Practices:**
#
#* **File Organization:** The code is separated into `main.tf` and `variables.tf` for better organization and maintainability.
#* **Resource Naming:** Resources are named consistently and descriptively, following Terraform naming conventions.
#* **Input Validation:** Input variables are defined with appropriate types and default values.
#* **Error Handling:** Timeouts are configured for resources to handle potential delays during creation, deletion, and updates.
#* **Sensitive Data:** The `pgpassword` and `my_password` variables are marked as sensitive to prevent accidental exposure.
#
#**4. Potential Optimizations:**
#
#* **Modules:** The code could be further optimized by using Terraform modules to encapsulate common configurations (e.g., a module for creating storage buckets with uniform access and viewer permissions).
#* **Output Variables:** Output variables could be defined in `outputs.tf` to easily retrieve important information (e.g., the URLs of the Cloud Run services).
#* **Terraform State:** The Terraform state should be managed using a remote backend (e.g., Google Cloud Storage or Terraform Cloud) to ensure consistency and prevent accidental state loss.
#
#**Example Usage:**
#
#```bash
## Set the project ID and other variables
#export PROJECT_ID=your-project-id
#export PGPASSWORD=your-pgpassword
#export MY_PASSWORD=your-my-password
#export MY_USER=your-my-user
#export DB_URL=jdbc:postgresql://your-db-url
#
## Initialize Terraform
#terraform init
#
## Plan the changes
#terraform plan
#
## Apply the changes
#terraform apply
#```
#
#This example demonstrates how to set environment variables, initialize Terraform, plan the changes, and apply the Terraform configuration to create the GCP resources defined in the code.
