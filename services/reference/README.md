# Optimize Serverless Apps In Google Cloud - Reference Service

### Create a Spring Boot Application

```
# Note: subject to change!
git clone https://github.com/ddobrin/serverless-production-readiness-java-gcp.git

# Note: subject to change!
cd prod/reference
```

### Validate that you have Java 17 and Maven installed
```shell
java -version

./mvnw --version
```
### Validate that GraalVM for Java is installed if building native images
```shell
java -version

# should indicate or later version
java version "17.0.7" 2023-04-18 LTS
Java(TM) SE Runtime Environment Oracle GraalVM 17.0.7+8.1 (build 17.0.7+8-LTS-jvmci-23.0-b12)
Java HotSpot(TM) 64-Bit Server VM Oracle GraalVM 17.0.7+8.1 (build 17.0.7+8-LTS-jvmci-23.0-b12, mixed mode, sharing)
```
### Validate that the starter app is good to go
```
./mvnw clean package spring-boot:run
```

From a terminal window, test the app
```
curl localhost:8085/metadata
```

### Validate that GraalVM for Java is installed if building native images
```shell
java -version

# should indicate or later version
java version "17.0.7" 2023-04-18 LTS
Java(TM) SE Runtime Environment Oracle GraalVM 17.0.7+8.1 (build 17.0.7+8-LTS-jvmci-23.0-b12)
Java HotSpot(TM) 64-Bit Server VM Oracle GraalVM 17.0.7+8.1 (build 17.0.7+8-LTS-jvmci-23.0-b12, mixed mode, sharing)
```
### Build a JVM and Native Java application image
```
./mvnw clean package 

./mvnw native:compile -Pnative
```

### Build a JVM and Native Java Docker Image
```
./mvnw spring-boot:build-image -Pjit

./mvnw spring-boot:build-image -Pnative
```

### Start your app with AOT enabled
```shell
java -Dspring.aot.enabled -jar target/reference-1.0.0.jar
```

### Deploy Docker images to Cloud Run

Check existing deployed Cloud Run Services
```shell
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   $PROJECT_ID

gcloud run services list
```

Deploy the Reference JIT image
```shell
gcloud run deploy reference \
     --image gcr.io/${PROJECT_ID}/reference \
     --region us-central1 \
     --memory 2Gi --allow-unauthenticated
     
gcloud run deploy reference-docker \
     --image gcr.io/${PROJECT_ID}/reference-docker \
     --region us-central1 \
     --memory 2Gi --allow-unauthenticated     
```

Deploy the Quotes Native Java image
```shell
gcloud run deploy reference-native \
     --image gcr.io/${PROJECT_ID}/reference-native \
     --region us-central1 \
     --memory 2Gi --allow-unauthenticated
```

### Test the application

Test the application locally
```shell
curl --location 'http://localhost:8087/metadata' 
```

Test the application in Cloud Run
```shell
TOKEN=$(gcloud auth print-identity-token)

# Get the URL of the deployed service
# Test JIT image
http -A bearer -a $TOKEN  https://<BASE_URL>/metadata

# Test JIT image - Docker image built with Dockerfile
http -A bearer -a $TOKEN  <BASE_URL>/metadata

# Test Native Java image
http -A bearer -a $TOKEN <BASE_URL>/metadata
```