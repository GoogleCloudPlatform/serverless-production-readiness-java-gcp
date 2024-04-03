terraform {
  required_providers {
    google = {
      version = ">3.5.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# Enable required Google Cloud services
module "project_services" {
  source  = "terraform-google-modules/project-factory/google//modules/project_services"
  version = "~> 11.1"

  project_id = var.project_id
  disable_services_on_destroy = false
  disable_dependent_services  = false

  activate_apis = [
    "vision.googleapis.com",
    "cloudfunctions.googleapis.com",
    "cloudbuild.googleapis.com",
    "run.googleapis.com",
    "logging.googleapis.com",
    "storage-component.googleapis.com",
    "aiplatform.googleapis.com",
    "run.googleapis.com",           # Cloud Run API
    "alloydb.googleapis.com"        # Assuming this is the AlloyDB API name; please verify
  ]
}

# Dynamic creation of Google Cloud Storage buckets
resource "google_storage_bucket" "dynamic_buckets" {
  for_each = var.buckets

  name          = each.key
  location      = each.value.location
  force_destroy = each.value.force_destroy
  uniform_bucket_level_access = each.value.uniform_bucket_level_access
}

resource "google_storage_bucket_iam_binding" "dynamic_buckets_public" {
  for_each = var.buckets

  bucket = each.key
  role   = "roles/storage.objectViewer"
  members = [
    "allUsers",
  ]
}

resource "google_compute_network" "auto_vpc" {
  name                    = "default"
  depends_on = [module.project_services]
  auto_create_subnetworks = true # This creates subnets in each region, similar to a default VPC
}

# Reserve IP range for VPC peering
resource "google_compute_global_address" "psa_range" {
  name          = "psa-range"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 24
  network       = "default"
}

# VPC Connector
resource "google_vpc_access_connector" "alloy_connector" {
  name          = "alloy-connector"
  region        = var.region
  network       = "default"
  ip_cidr_range = "10.100.0.0/28"
  depends_on = [module.auto_vpc]
}

locals {
  cloud_run_services = {
    "books-genai-jit" = {
      image = "us-docker.pkg.dev/${var.project_id}/books-genai-jit/books-genai:latest"
    },
    "books-genai-native" = {
      image = "us-docker.pkg.dev/${var.project_id}/books-genai-native/books-genai:latest"
    }
  }
}

# Example Cloud Run deployment
resource "google_cloud_run_service" "cloud_run" {
  for_each = local.cloud_run_services

  name     = each.key
  location = var.region

  template {
    metadata {
      annotations = {
        "run.googleapis.com/cpu"    = "4"
        "run.googleapis.com/memory" = "4Gi"
      }
    }
    spec {
      containers {
        image = each.value.image

        resources {
          limits = {
            cpu    = "4000m"
            memory = "4Gi"
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
          name  = "VERTEX_AI_GEMINI_PROJECT_ID"
          value = var.project_id
        }
        env {
          name  = "VERTEX_AI_GEMINI_LOCATION"
          value = var.region
        }
        env {
          name  = "JAVA_TOOL_OPTIONS"
          value = "-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:ActiveProcessorCount=4 -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xss256k"
        }
        // Add more environment variables as necessary
      }
    }
  }

  traffic {
    percent         = 100
    latest_revision = true
  }

  autogenerate_revision_name = true
  allow_unauthenticated      = true
}

# Eventarc trigger example (adjust according to your actual setup)
resource "google_eventarc_trigger" "books_genai_jit_trigger_image" {
  name        = "books-genai-jit-trigger-image"
  location    = var.region
  service_account = "your-service-account@your-project.iam.gserviceaccount.com"

  destination {
    cloud_run {
      service = google_cloud_run_service.books_genai_jit.name
      path    = "/images"
    }
  }

  matching_criteria {
    attribute = "type"
    value     = "google.cloud.storage.object.v1.finalized"
  }

  transport {
    pubsub {
      topic = "projects/${var.project_id}/topics/your-topic"
    }
  }
}

resource "null_resource" "alloydb_cluster" {
  triggers = {
    always_run = "${timestamp()}"
  }

  provisioner "local-exec" {
    command = "gcloud alloydb clusters create ${var.alloydb_cluster_name} --region=${var.region} --password=${var.alloydb_password}"
  }
}