# Custom Commands - Explain, Plan, Implement, Deploy

This folder is a collection of custom commands for the [Google Gemini CLI](https://github.com/google-gemini/gemini-cli)

## 🚀 How to Use

You can install these configurations globally for all projects or locally for a single project.

**Global Installation:**

Copy the `commands` folder from the [repo](https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp/tree/main/genai/gemini-cli-extensions/commands) to the root folder of your machine under `~/.gemini/commands/`
```bash
git clone --depth 1 https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp.git ~/.gemini-tmp && 
rsync -av ~/.gemini-tmp/genai/gemini-cli-extensions/commands/ ~/.gemini/commands && rm -rf ~/.gemini-tmp
```

**Project-Specific Installation:**

Copy the `commands` folder from the [repo](https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp/tree/main/genai/gemini-cli-extensions/commands) to the `.gemini/commands/` folder of your project.
```bash
git clone --depth 1 https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp.git .gemini-tmp && 
rsync -av .gemini-tmp/genai/gemini-cli-extensions/commands/ ./.gemini/commands && rm -rf .gemini-tmp
```

**Note:** Both methods may overwrite existing configuration files with the same name.

## 🛠️ Custom Commands

Custom commands allow you to create powerful, reusable prompts. They are defined in TOML files and stored in a `commands` directory.

-   **Global commands:** `~/.gemini/commands/`
-   **Project-specific commands:** `<project>/.gemini/commands/`

## Available Demo Custom Commands in this folder

You can use these commands to assist you in the cycle of **exploring, planning, refining, implementing and deploying** code for your project

| Command | Description |
| :--- | :--- |
| explain | Explain mode. Analyzes the codebase to answer questions and provide insights |
| explain:interactive | Interactive Explore mode. Interactively explains the codebase through guided discovery.
|  |  
| plan:new | Plan mode. Generates a plan for a feature based on a description
| plan:refine | Refinement mode. Refines an existing plan based on user feedback.
| plan:impl | Implementation mode. Implements a plan for a feature based on a description
|  |  
| deploy:project | Deploys the project to a target environment

You can use this command to **generate a new GEMINI.md** file for your project

| Command | Description |
| :--- | :--- |
| generate:gemini_md | Analyzes the current project directory recursively and generate a comprehensive GEMINI.md file
