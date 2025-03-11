# Quotes Service - Build & Deploy to Cloud Run
## Connect to Google Gemini, open-model LLM in VertexAI and open-model LLM in GKE

This sample is building on the materials from the [Quotes sample](https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp/blob/main/services/quotes/README.md) and adds a simple UI for interacting with the service.

The UI is built on the [Vaadin Hilla](https://hilla.dev/) framework. Hilla seamlessly connects Spring Boot and React to accelerate application development.

# Before you start
Run with the following environment variables set
```shell
    # LLM in VertexAI env
    export VERTEX_AI_PROJECT_ID=<your project id>
    export VERTEX_AI_LOCATION=us-central1
    export VERTEX_AI_MODEL=<your model in Vertex>
    # ex: export VERTEX_AI_MODEL=meta/llama3-405b-instruct-maas
    
    # Gemini in VertexAI env
    export OPENAI_API_KEY=abc
    # the value is derived programatically, however Spring AI auto-config requries it
    export VERTEX_AI_GEMINI_PROJECT_ID=<your project id>
    export VERTEX_AI_GEMINI_LOCATION=us-central1
    export VERTEX_AI_GEMINI_MODEL=<your Gemini model>
    # ex: export VERTEX_AI_GEMINI_MODEL=gemini-2.0-flash-001
```
or set them locally in the application.properties file:
```
#################################
# Google Vertex AI Gemini
#################################
spring.ai.vertex.ai.gemini.project-id=${VERTEX_AI_GEMINI_PROJECT_ID}
spring.ai.vertex.ai.gemini.location=${VERTEX_AI_GEMINI_LOCATION}
spring.ai.vertex.ai.gemini.chat.options.model=${VERTEX_AI_GEMINI_MODEL}
spring.ai.vertex.ai.gemini.transport=grpc

#################################
# OpenAI VertexAI - manual configuration
#################################
spring.ai.vertex.ai.vertex.ai.gemini.project-id=${VERTEX_AI_PROJECT_ID}
spring.ai.vertex.ai.vertex.ai.gemini.location=${VERTEX_AI_LOCATION}
spring.ai.openai.vertex.ai.chat.options.model=${VERTEX_AI_MODEL}
spring.ai.openai.vertex.ai.chat.base-url=https://${VERTEX_AI_LOCATION}-aiplatform.googleapis.com/v1beta1/projects/${VERTEX_AI_PROJECT_ID}/locations/${VERTEX_AI_LOCATION}/endpoints/openapi
spring.ai.openai.vertex.ai.chat.completions-path=/chat/completions
spring.ai.openai.vertex.ai.chat.options.max-tokens=1024
```
# Build

### Create a Spring Boot Application
```shell
# clone the repo
git clone https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp.git

# app available here
cd serverless-production-readiness-java-gcp/ai-patterns/spring-ai-quotes-llm-in-gke
```

### Validate that you have Java 21 and Maven installed
```shell
# install Java 21 if it is not installed in your env or cloudshell
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.4-graal 

# Select Y to set as default or use
sdk use java 21.0.4-graal

# validate our Java runtime
java -version

# observe the output
java version "21.0.4" 2024-07-16 LTS
Java(TM) SE Runtime Environment Oracle GraalVM 21.0.4+8.1 (build 21.0.4+8-LTS-jvmci-23.1-b41)
Java HotSpot(TM) 64-Bit Server VM Oracle GraalVM 21.0.4+8.1 (build 21.0.4+8-LTS-jvmci-23.1-b41, mixed mode, sharing)
```

### Build a Java application image
```
# build the app
./mvnw clean package -Pproduction -DskipTests

# start the app
java -jar target/quotes-llm-1.0.0.jar
```

Test the application locally:
```
# Access the app in a browser window
http://localhost:8083

# Test from a terminal
curl localhost:8083/random-quote 
curl localhost:8083/random-quote-llm
```

### Build a Docker image with Dockerfiles
```shell
# build an image with a fat JAR
docker build -f ./containerize/Dockerfile-fatjar -t quotes-fatjar .
```
### Build a Java Docker Image with Buildpacks
```
./mvnw spring-boot:build-image -DskipTests -Pproduction -Dspring-boot.build-image.imageName=quotes-llm
```

### Test the locally built images on the local machine
```shell
docker run --rm -p 8080:8083 quotes-llm
```

# Deploy
### Tag and push images to a registry
If you have built the image locally, tag it first and push to a container registry
```shell
# tag the image
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   $PROJECT_ID

# set the region for the image
export REGION=<your region>
ex:
export REGION=us-central1
echo $REGION

# tag and push image to Artifact Registry
gcloud artifacts repositories create quotes-llm \
      --repository-format=docker \
      --location=us-central1 \
      --description="Quote app images accessing LLMs" \
      --immutable-tags \
      --async
gcloud auth configure-docker us-central1-docker.pkg.dev

docker tag quotes-llm ${REGION}-docker.pkg.dev/${PROJECT_ID}/quotes-llm/quotes-llm
docker push ${REGION}-docker.pkg.dev/${PROJECT_ID}/quotes-llm/quotes-llm
```

### Deploy Docker images to Cloud Run

Check existing deployed Cloud Run Services
```shell
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   $PROJECT_ID

gcloud run services list
```

Deploy the Quotes JIT image
```shell
# note the URL of the deployed service
gcloud run deploy quotes-llm \
     --image ${REGION}-docker.pkg.dev/${PROJECT_ID}/quotes-llm/quotes-llm \
     --region us-central1 \
     --memory 2Gi --cpu=2 \
     --execution-environment gen1 \
     --set-env-vars=SERVER_PORT=8080 \
     --set-env-vars=JAVA_TOOL_OPTIONS='-XX:+UseG1GC -XX:MaxRAMPercentage=80 -XX:ActiveProcessorCount=2 -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xss256k' \
     --set-env-vars=VERTEX_AI_GEMINI_PROJECT_ID=${PROJECT_ID} \
     --set-env-vars=VERTEX_AI_GEMINI_LOCATION=${REGION} \
     --set-env-vars=VERTEX_AI_GEMINI_MODEL=gemini-1.5-pro-001 \
     --set-env-vars=VERTEX_AI_PROJECT_ID=${PROJECT_ID} \
     --set-env-vars=VERTEX_AI_LOCATION=${REGION} \
     --set-env-vars=VERTEX_AI_MODEL=meta/llama3-405b-instruct-maas \
     --set-env-vars=OPENAI_API_KEY=${OPENAI_API_KEY} \
     --cpu-boost \
     --allow-unauthenticated 
     
# observe the URL, use it for UI or cURL access
# example:
...
Service [quotes-llm] revision [quotes-llm-00008-wq5] has been deployed and is serving 100 percent of traffic.
Service URL: https://quotes-llm-....-uc.a.run.app     
```

Test the application in Cloud Run:
``````
# Test from a terminal
curl https://quotes-llm-6hrf...-uc.a.run.app/random-quote
curl https://quotes-llm-6hrf...-uc.a.run.app/random-quote-llm
curl https://quotes-llm-6hrf...-uc.a.run.app/random-quote-llm-vertex

# Access the app in a browser window
https://quotes-llm-6hr...-uc.a.run.app
```