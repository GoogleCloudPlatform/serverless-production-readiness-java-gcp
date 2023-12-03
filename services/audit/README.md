# Audit Service - JIT and Native Java Build & Deployment to Cloud Run

# Build

### Create a Spring Boot Application
```
# clone the repo
git clone https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp.git
cd services/audit

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
curl localhost:8084/start

# Output
Hello from your local environment!
```

### Start your app with AOT enabled
```shell
java -Dspring.aot.enabled=true -jar target/audit-1.0.0.jar
```

### Build a JIT and Native Java application image
```
./mvnw package -DskipTests 

./mvnw native:compile -Pnative -DskipTests
```

### Start your app with AOT enabled
```shell
java -Dspring.aot.enabled -jar target/audit-1.0.0.jar
```
### Build a JIT Docker image with Dockerfiles
```shell
# build an image with jlink
docker build . -f ./containerize/Dockerfile-jlink -t audit-jlink

# build an image with a fat JAR
docker build -f ./containerize/Dockerfile-fatjar -t audit-fatjar .

# build an image with custom layers
docker build -f ./containerize/Dockerfile-custom -t audit-custom .
```
### Build a JIT and Native Java Docker Image with Buildpacks
```
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=audit

./mvnw spring-boot:build-image  -DskipTests -Pnative -Dspring-boot.build-image.imageName=audit-native
```

### Test the locally built images on the local machine
```shell
docker run --rm -p 8080:8084 audit

docker run --rm -p 8080:8084 audit-native
```

### Build, test with CloudBuild in Cloud Build
```shell
gcloud builds submit  --machine-type E2-HIGHCPU-32

gcloud builds submit  --machine-type E2-HIGHCPU-32 --config cloudbuild-native.yaml
```

#Deploy
### Tag and push images to a registry
If you have built the image locally, tag it first and push to a container registry
```shell
# tag the image
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   $PROJECT_ID

# tag and push JIT image
docker tag audit gcr.io/${PROJECT_ID}/audit
docker push gcr.io/${PROJECT_ID}/audit

# tag and push Native image
docker tag audit-native gcr.io/${PROJECT_ID}/audit-native
docker push gcr.io/${PROJECT_ID}/audit-native
```

### Deploy Docker images to Cloud Run

Check existing deployed Cloud Run Services
```shell
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   $PROJECT_ID

gcloud run services list
```

Deploy the Audit JIT image
```shell
# note the URL of the deployed service
gcloud run deploy audit \
     --image gcr.io/${PROJECT_ID}/audit \
     --region us-central1 \
     --memory 2Gi --allow-unauthenticated
```

Deploy the Audit Native Java image
```shell
# note the URL of the deployed service
gcloud run deploy audit-native \
     --image gcr.io/${PROJECT_ID}/audit-native \
     --region us-central1 \
     --memory 2Gi --allow-unauthenticated
```

### Testing the Audit app

Start the containerized app locally:
```shell
# validate that app has started
curl localhost:8080:/start

curl --location 'http://localhost:8080' \
--header 'ce-id: test id' \
--header 'ce-source: test source' \
--header 'ce-type: test type' \
--header 'ce-specversion: test specversion' \
--header 'ce-subject: test subject' \
--header 'Content-Type: application/json' \
--data '{
    "message": {
        "randomId": "1fdcc71b-42d9-4a98-aa64-941aae4957f1",
        "quote": "test quote",
        "author": "anonymous",
        "book": "new book",
        "attributes": {}
    }
}'
```

Test Audit application in Cloud Run
```shell
# find the Audit URL if you have not noted it
gcloud run services list | grep audit
✔  audit                     us-central1   https://audit-...-uc.a.run.app             
✔  audit-native              us-central1   https://audit-native-...-uc.a.run.app

# validate that app passes start-up check
curl <URL>:/start

curl  \
--header 'ce-id: test id' \
--header 'ce-source: test source' \
--header 'ce-type: test type' \
--header 'ce-specversion: test specversion' \
--header 'ce-subject: test subject' \
--header 'Content-Type: application/json' \
--data '{
    "message": {
        "randomId": "1fdcc71b-42d9-4a98-aa64-941aae4957f1",
        "quote": "test quote",
        "author": "anonymous",
        "book": "new book",
        "attributes": {}
    }
}' -X POST https://audit-...-uc.a.run.app
       
```

If you have deployed the app with security enabled, (no --allow-unauthenticated flag) you can test it with a Bearer token. You can use also an alternative [HTTP test client](https://httpie.io/)
```shell
TOKEN=$(gcloud auth print-identity-token)

# Get the URL of the deployed service
# Test JIT image with HTTPie
http -A bearer -a $TOKEN  https://<BASE_URL>/start

curl -H "Authorization: Bearer $(gcloud auth print-identity-token)" \
--header 'ce-id: test id' \
--header 'ce-source: test source' \
--header 'ce-type: test type' \
--header 'ce-specversion: test specversion' \
--header 'ce-subject: test subject' \
--header 'Content-Type: application/json' \
--data '{
    "message": {
        "randomId": "1fdcc71b-42d9-4a98-aa64-941aae4957f1",
        "quote": "test quote",
        "author": "anonymous",
        "book": "new book",
        "attributes": {}
    }
}' -X POST https://audit-...-uc.a.run.app
```
