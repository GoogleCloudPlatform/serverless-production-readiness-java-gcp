# Spring AI Structured Output Demo

Run with the following environment variables set
* VERTEX_AI_GEMINI_PROJECT_ID=<your project>>
* VERTEX_AI_GEMINI_MODEL=gemini-1.5-pro-001
* VERTEX_AI_GEMINI_LOCATION=<region>

* Ex.: VERTEX_AI_PROJECT_ID=my-project;VERTEX_AI_LOCATION=us-central1

Build and run from the CLI:
```shell
./mvnw clean package

 java -jar target/spring-ai-structured-output-1.0.0.jar
```
