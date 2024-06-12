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