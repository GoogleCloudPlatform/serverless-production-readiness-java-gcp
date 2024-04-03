project_id = "your-gcp-project-id"
my_user = "example_user"
db_url = "jdbc:your_database_url"
my_password = ""
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