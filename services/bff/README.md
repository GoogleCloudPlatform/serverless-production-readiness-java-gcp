# Optimize Serverless Apps In Google Cloud - BFF Service

### Create a Spring Boot Application

```
# Note: repository location subject to change!
git clone https://github.com/ddobrin/serverless-production-readiness-java-gcp.git

# Note: subject to change!
cd prod/bff
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
curl localhost:8080

# Output
Hello from your local environment!
```

### Build a JVM and Native Java application image
```
./mvnw clean package 

./mvnw clean package -Pnative -DskipTests
```

### Build a JVM and Native Java Docker Image
```
./mvnw clean package -Pjvm-image -DskipTests

./mvnw clean package -Pnative-image -DskipTests
```

### Start your app with AOT enabled
```shell
java -Dspring.aot.enabled -jar target/bff-1.0.0.jar
```

### Build a JIT and Native Java Docker Image with Buildpacks
```
./mvnw spring-boot:build-image -Pjit -DskipTests

./mvnw spring-boot:build-image -Pnative -DskipTests
```

### Build a JIT and Native Java Docker Image with Buildpacks
```
./mvnw spring-boot:build-image -Pjit -DskipTests

./mvnw spring-boot:build-image -Pnative -DskipTests
```

### Build, test with CloudBuild in Cloud Build
```shell
gcloud builds submit  --machine-type E2-HIGHCPU-32

gcloud builds submit --config cloudbuild-docker.yaml --machine-type E2-HIGHCPU-32

gcloud builds submit  --config cloudbuild-native.yaml --machine-type E2-HIGHCPU-32 
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
