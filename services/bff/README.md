# BFF Service - JIT and Native Java Build & Deployment to Cloud Run

# Build

### Create a Spring Boot Application
```
# clone the repo
git clone https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp.git
cd services/quotes

# Note: 
# main branch - Java 17 code level
# java21 branch - Java 21 code level
git checkout java21
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
./mvnw package spring-boot:run
```

From a terminal window, test the app
```
curl localhost:8080

# Output
Hello from your local environment!
```

### Build a JVM and Native Java application image
```
./mvnw clean package 

./mvnw clean package -Pnative -DskipTests
```
### Build a JVM and Native Java application image
```
./mvnw package -DskipTests 

./mvnw native:compile -Pnative -DskipTests
```
### Start your app with AOT enabled
```shell
java -Dspring.aot.enabled -jar target/bff-1.0.0.jar
```

### Build a JIT and Native Java Docker Image with Buildpacks
```
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=bff

./mvnw spring-boot:build-image  -DskipTests -Pnative -Dspring-boot.build-image.imageName=bff-native
```

### Test the locally built images on the local machine
```shell
docker run --rm -p 8080:8080 bff

docker run --rm -p 8080:8080 bff-native
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
docker tag bff gcr.io/${PROJECT_ID}/bff
docker push gcr.io/${PROJECT_ID}/bff

# tag and push Native image
docker tag bff-native gcr.io/${PROJECT_ID}/bff-native
docker push gcr.io/${PROJECT_ID}/bff-native
```

### Deploy Docker images to Cloud Run

Check existing deployed Cloud Run Services
```shell
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   $PROJECT_ID

gcloud run services list
```

Deploy the BFF JIT image
```shell
gcloud run deploy bff \
     --image gcr.io/${PROJECT_ID}/bff \
     --region us-central1 \
     --memory 2Gi --allow-unauthenticated  
```

Deploy the BFF Native Java image
```shell
gcloud run deploy bff-native \
     --image gcr.io/${PROJECT_ID}/bff-native \
     --region us-central1 \
     --memory 2Gi --allow-unauthenticated
```

### Test the application

Test the application locally
```shell
# read the existing Quotes
curl --location 'http://localhost:8080/quotes'

# add an additional Quote
curl --location 'http://localhost:8080/quotes' --header 'Content-Type: application/json' --data '{
    "author" : "Isabel Allende",
    "quote" : "The longer I live, the more uninformed I feel. Only the young have an explanation for everything.",
    "book" : "City of the Beasts"
}'

# Re-read the list of Quotes
curl --location 'http://localhost:8080/quotes'
```

Test the application in Cloud Run
```shell
TOKEN=$(gcloud auth print-identity-token)

# Copy the URL of the deployed app
# Test JIT image
http -A bearer -a $TOKEN  https://<BASE_URL>/quotes
```
