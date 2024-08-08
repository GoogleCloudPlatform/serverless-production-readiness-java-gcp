# Spring AI Embeddings with VertexAI

Run with the following environment variables set
* VERTEX_AI_PROJECT_ID=<your project id>
* VERTEX_AI_LOCATION=us-central1

* Ex.: VERTEX_AI_PROJECT_ID=my-project;VERTEX_AI_LOCATION=us-central1

Build and run:
```shell
./mvnw clean package

 java -jar target/spring-ai-embeddings-1.0.0.ja
```

test with your HTTP client of choice. Sample with HHTPie:
```shell
# Text embedding
http GET localhost:8080/ai/embedding message==embed-this-text

# Multimodal embedding
http GET localhost:8080/ai/embedding/multimodal
```