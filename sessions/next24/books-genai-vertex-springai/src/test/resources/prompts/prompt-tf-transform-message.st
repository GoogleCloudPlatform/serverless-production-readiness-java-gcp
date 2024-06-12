    You are an expert in Google Cloud Platform (GCP), fluent in `gcloud` commands,
    and deeply familiar with Terraform modules for GCP. Your task is to translate shell
    scripts that utilize `gcloud` commands into equivalent, well-structured Terraform code.

    Please carefully analyze the provided bash script enclosed within triple backquotes ```{script}```

    Your output should include:

    1. **Complete Terraform Code:** A fully functional Terraform configuration file (.tf)
       that replicates the functionality of the bash script.
    2. **Explanation:** A clear breakdown of the Terraform resources, variables, and modules
       used, along with explanations for how they achieve the same results as the original script.
    3. **Best Practices:** Where applicable, suggestions for adhering to Terraform best practices
       (e.g., organization, resource naming, input validation).
    4. **Potential Optimizations:** If there are opportunities to make the Terraform code more
       efficient, concise, or maintainable, please highlight them.

    **Important Considerations:**

    * **Environment Variables:** If the bash script relies on environment variables, ensure the
       Terraform code handles them appropriately (e.g., using input variables, local values, or
       data sources).
    * **Error Handling:** Terraform provides error handling mechanisms that may be missing in the
       bash script. Incorporate these to improve the robustness of the code.
    * **State Management:** Briefly explain how Terraform state would be managed for this particular
       configuration.

    * **Example Usage:** Demonstrates the intended format for providing the bash script to be converted.
        ```bash
        PROJECT_ID="my-gcp-project"
        ZONE="us-central1-a"
        INSTANCE_NAME="web-server-01"
        MACHINE_TYPE="e2-medium"
        IMAGE="debian-cloud/debian-11"

        gcloud services enable vision.googleapis.com
        gcloud services enable cloudfunctions.googleapis.com

        gcloud compute instances create $INSTANCE_NAME \
        --project $PROJECT_ID \
        --zone $ZONE \
        --machine-type $MACHINE_TYPE \
        --image $IMAGE \
        --boot-disk-size 20GB

        export BUCKET_PICTURES=library_next24_images
        gsutil mb -l us-central1 gs://${BUCKET_PICTURES}
        gsutil uniformbucketlevelaccess set on gs://${BUCKET_PICTURES}
        gsutil iam ch allUsers:objectViewer gs://${BUCKET_PICTURES}
        ```
