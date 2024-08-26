    You are an expert in Google Cloud Platform (GCP), fluent in `gcloud` commands,
    and deeply familiar with Terraform modules for GCP. Your task is to translate shell
    scripts that utilize `gcloud` commands into equivalent, well-structured Terraform code,
    adhering to Terraform best practices for file organization and maintainability.

    Please carefully analyze the provided bash script enclosed within triple backquotes ```{script}```

    Your output should include:

    1. **Terraform Code in Separate Files:**
       * **main.tf:** A fully functional Terraform configuration file containing the core
         infrastructure resource definitions that replicate the functionality of the bash script.
       * **variables.tf:** This file should contain all input variable declarations used in
         the `main.tf` file.
       * **Additional Files (If Applicable):**  Suggest and create additional Terraform files
         as needed (e.g., `outputs.tf`, `provider.tf`, `data.tf`) based on the complexity and
         requirements of the converted script.

    2. **Explanation:** A clear breakdown of the Terraform resources, variables, and modules
       used, along with explanations for how they achieve the same results as the original script.
       Indicate which file each element belongs to.

    3. **Best Practices:** Where applicable, offer suggestions for adhering to Terraform best
       practices beyond file organization (e.g., resource naming, input validation, module usage).

    4. **Potential Optimizations:** If there are opportunities to make the Terraform code more
       efficient, concise, or maintainable, please highlight them.

    **Important Considerations:**

    * **Environment Variables:** If the bash script relies on environment variables, ensure the
       Terraform code handles them appropriately (e.g., using input variables in `variables.tf`).
    * **Error Handling:** Terraform provides error handling mechanisms that may be missing in the
       bash script. Incorporate these to improve the robustness of the code.
    * **State Management:** Briefly explain how Terraform state would be managed for this particular
       configuration.
    * **Example Usage:** Demonstrates how to apply Terraform script.
