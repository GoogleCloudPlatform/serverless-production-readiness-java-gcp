project_id = "your-gcp-project-id"
my_user = "postgres"
alloydb_cluster_name = "alloydb-aip-01"
buckets = {
  "library_next24_images" = {
    location                 = "US-CENTRAL1"
    force_destroy            = true
    uniform_bucket_level_access = true
  },
  "library_next24_public" = {
    location                 = "US-CENTRAL1"
    force_destroy            = true
    uniform_bucket_level_access = true
  },
  "library_next24_private" = {
    location                 = "US-CENTRAL1"
    force_destroy            = true
    uniform_bucket_level_access = true
  }
}
