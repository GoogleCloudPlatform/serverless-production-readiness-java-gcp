# Reference Service - JIT and Native Java Build & Deployment to Cloud Run

# Build

### Create a Spring Boot Application
```
# clone the repo
git clone https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp.git
cd services/reference
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
curl localhost:8085/metadata
```

### Build a JIT and Native Java application image
```
./mvnw package 

./mvnw native:compile -Pnative
```
### Start your app with AOT enabled
```shell
java -Dspring.aot.enabled=true -jar target/reference-1.0.0.jar
```

### Build a JIT and Native Java Docker Image with Buildpacks
```
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=reference

./mvnw spring-boot:build-image  -DskipTests -Pnative -Dspring-boot.build-image.imageName=reference-native
```

### Test the locally built images on the local machine
```shell
docker run --rm -p 8080:8085 reference

docker run --rm -p 8080:8085 reference-native
```

### Deploy Docker images to Cloud Run

Check existing deployed Cloud Run Services
```shell
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   $PROJECT_ID

gcloud run services list
```

# Deploy
### Tag and push images to a registry
If you have built the image locally, tag it first and push to a container registry
```shell
# tag the image
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   $PROJECT_ID

# tag and push JIT image
docker tag reference gcr.io/${PROJECT_ID}/reference
docker push gcr.io/${PROJECT_ID}/reference

# tag and push Native image
docker tag reference-native gcr.io/${PROJECT_ID}/reference-native
docker push gcr.io/${PROJECT_ID}/reference-native
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
# note the URL of the deployed app
gcloud run deploy reference \
     --image gcr.io/${PROJECT_ID}/reference \
     --region us-central1 \
     --memory 2Gi --allow-unauthenticated
```

Deploy the Reference Native Java image
```shell
# note the URL of the deployed app
gcloud run deploy reference-native \
     --image gcr.io/${PROJECT_ID}/reference-native \
     --region us-central1 \
     --memory 2Gi --allow-unauthenticated
```

### Test the application
Start the containerized app locally:
```shell
curl --location 'http://localhost:8087/metadata' 
```

Test the application in Cloud Run
```shell
# find the Reference URL is you have not noted it
gcloud run services list | grep reference
reference                                               latest              072323ccd2f7   43 years ago    366MB

# validate that app passes start-up check
curl <URL>:/start

# retrieve the metadata from the URL
curl <URL>:/metadata

````

If you have deployed the service with security enabled, (no --allow-unauthenticated flag) you can test it with a Bearer token. You can use also an alternative [HTTP test client](https://httpie.io/)
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