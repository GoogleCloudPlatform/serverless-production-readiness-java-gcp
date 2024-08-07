# Quotes Service - JIT and Native Java Build & Deployment to Cloud Run
## Connect to Google Gemini, open-model LLM in VertexAI and open-model LLM in GKE

This sample is building on the materials from the [Quotes sample](../quotes/README.md) and adds a simple UI for interacting with the service.

The UI is built on the [Vaadin Hilla](https://hilla.dev/) framework. Hilla seamlessly connects Spring Boot and React to accelerate application development.

# Before you start
Run with the following environment variables set
```shell
    # LLM in VertexAI env
    export VERTEX_AI_PROJECT_ID=<your project id>
    export VERTEX_AI_LOCATION=us-central1
    export VERTEX_AI_MODEL=<your model in Vertex>
    # ex: export VERTEX_AI_MODEL=meta/llama3-405b-instruct-maas
    
    # LLM in GKE env
    export OPENAI_API_KEY=<you API key for the LLM in GKE>
    export OPENAI_API_GKE_IP=<IP for deployed model>
    export OPENAI_API_GKE_MODEL=<your model in Vertex>
    # ex: export OPENAI_API_GKE_MODEL=meta-llama/Meta-Llama-3.1-8B-Instruct
    
    # Gemini in VertexAI env
    export VERTEX_AI_GEMINI_PROJECT_ID=<your project id>
    export VERTEX_AI_GEMINI_LOCATION=us-central1
    export VERTEX_AI_GEMINI_MODEL=<your Gemini model>
    # ex: export VERTEX_AI_GEMINI_MODEL=gemini-1.5-pro-001
```
or set them locally in the application.properties file:
```java
#################################
# Google Vertex AI Gemini
#################################
spring.ai.vertex.ai.gemini.project-id=${VERTEX_AI_GEMINI_PROJECT_ID}
spring.ai.vertex.ai.gemini.location=${VERTEX_AI_GEMINI_LOCATION}
spring.ai.vertex.ai.gemini.chat.options.model=${VERTEX_AI_GEMINI_MODEL}
spring.ai.vertex.ai.gemini.transport=grpc

#################################
# OpenAI API - LLM in GKE
#################################
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.options.model=${OPENAI_API_GKE_MODEL}
spring.ai.openai.chat.base-url=${OPENAI_API_GKE_IP}
spring.ai.openai.chat.options.max-tokens=1024

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
```
# clone the repo
git clone https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp.git
cd ai-patterns/spring-ai-quotes-llm-in-gke
```

### Validate that you have Java 21 and Maven installed
```shell
java -version
```

### Validate that GraalVM for Java is installed if building native images
```shell
java -version

# should indicate this or later version
java version "21" 2023-09-19
Java(TM) SE Runtime Environment Oracle GraalVM 21+35.1 (build 21+35-jvmci-23.1-b15)
Java HotSpot(TM) 64-Bit Server VM Oracle GraalVM 21+35.1 (build 21+35-jvmci-23.1-b15, mixed mode, sharing)
```

### Validate that the starter app is good to go
```
 ./mvnw clean package -Pproduction
```

From a terminal window, test the app
```
curl localhost:8083/start

# Output
Hello from your local environment!
```

### Build a JIT and Native Java application image
```
./mvnw package -DskipTests -Pproduction

./mvnw native:compile -Pnative -DskipTests -Pproduction
```

### Start your app with AOT enabled
```shell
java -Dspring.aot.enabled=true -jar target/quotes-1.0.0.jar
```
### Build a JIT Docker image with Dockerfiles
```shell
# build an image with jlink
docker build . -f ./containerize/Dockerfile-jlink -t quotes-jlink

# build an image with a fat JAR
docker build -f ./containerize/Dockerfile-fatjar -t quotes-fatjar .

# build an image with custom layers
docker build -f ./containerize/Dockerfile-custom -t quotes-custom .
```
### Build a JIT and Native Java Docker Image with Buildpacks
```
./mvnw spring-boot:build-image -DskipTests -Pproduction -Dspring-boot.build-image.imageName=quotes-llm

./mvnw spring-boot:build-image -DskipTests -Pproduction -Pnative -Dspring-boot.build-image.imageName=quotes-native-llm
```

### Test the locally built images on the local machine
```shell
docker run --rm -p 8080:8083 quotes-llm

docker run --rm -p 8080:8083 quotes-native-llm
```
### Build, test with CloudBuild in Cloud Build
```shell
gcloud builds submit  --machine-type E2-HIGHCPU-32

gcloud builds submit --config cloudbuild-docker.yaml --machine-type E2-HIGHCPU-32

gcloud builds submit  --config cloudbuild-native.yaml --machine-type E2-HIGHCPU-32 
```
# Deploy
### Tag and push images to a registry
If you have built the image locally, tag it first and push to a container registry
```shell
# tag the image
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   $PROJECT_ID

# tag and push JIT image
docker tag quotes-llm gcr.io/${PROJECT_ID}/quotes-llm
docker push gcr.io/${PROJECT_ID}/quotes-llm

# tag and push Native image
docker tag quotes-native gcr.io/${PROJECT_ID}/quotes-native
docker push gcr.io/${PROJECT_ID}/quotes-native
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
     --image gcr.io/${PROJECT_ID}/quotes \
     --region us-central1 \
     --memory 2Gi --allow-unauthenticated
```

Deploy the Quotes Native Java image
```shell
# note the URL of the deployed service
gcloud run deploy quotes-native \
     --image gcr.io/${PROJECT_ID}/quotes-native \
     --region us-central1 \
     --memory 2Gi --allow-unauthenticated
```

### Test the application
Start the containerized app locally:
```shell
# validate that app has started
curl localhost:8080:/start

# get quotes from the app
curl localhost:8080/quotes
curl localhost:8080/random-quote

# add a new quote to the repository
curl --location 'http://localhost:8080/quotes' \
--header 'Content-Type: application/json' \
--data '{
    "author" : "Isabel Allende",
    "quote" : "The longer I live, the more uninformed I feel. Only the young have an explanation for everything.",
    "book" : "City of the Beasts"
}'
```

Test the application in Cloud Run
```shell
# find the Quotes URL is you have not noted it
gcloud run services list | grep quotes
✔  quotes                    us-central1   https://quotes-...-uc.a.run.app       
✔  quotes-native             us-central1   https://quotes-native-...-uc.a.run.app
# validate that app passes start-up check
curl <URL>:/start

# get quotes from the app
curl <URL>:/quotes
curl <URL>:/random-quote

# add a new quote to the repository
curl --location '<URL>:/quotes' \
--header 'Content-Type: application/json' \
--data '{
    "author" : "Isabel Allende",
    "quote" : "The longer I live, the more uninformed I feel. Only the young have an explanation for everything.",
    "book" : "City of the Beasts"
}'
```

If you have deployed the app with security enabled, (no --allow-unauthenticated flag) you can test it with a Bearer token. You can use also an alternative [HTTP test client](https://httpie.io/) 
```shell
TOKEN=$(gcloud auth print-identity-token)

# Get the URL of the deployed app
# Test JIT image
http -A bearer -a $TOKEN  https://<BASE_URL>/random-quote
http -A bearer -a $TOKEN  https://<BASE_URL>/quotes

# Test Native Java image
http -A bearer -a $TOKEN <BASE_URL>/random-quote
http -A bearer -a $TOKEN <BASE_URL>/quotes
```