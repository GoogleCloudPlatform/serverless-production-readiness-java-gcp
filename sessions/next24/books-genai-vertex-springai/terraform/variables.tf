variable "project_id" {
  type        = string
  description = "The GCP project ID."
}

variable "region" {
  type        = string
  default     = "us-central1"
  description = "The GCP region where resources will be created."
}

variable "buckets" {
  type = map(object({
    location                 = string
    force_destroy            = bool
    uniform_bucket_level_access = bool
  }))
  description = "Map of buckets to create with their attributes."
}

variable "alloydb_password" {
  type        = string
  description = "The password for the AlloyDB instance."
  sensitive   = true
}
variable "alloydb_cluster_name" {
  type        = string
  description = "The cluster name for the AlloyDB instance."
  sensitive   = true
}

variable "my_password" {
  description = "The password for MY application"
  type        = string
  sensitive   = true
}

variable "my_user" {
  description = "User for the application"
  type        = string
}

variable "db_url" {
  description = "Database URL"
  type        = string
}