# Quotes Service - JIT and Native Java Build & Deployment to Cloud Run

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
curl localhost:8083/start

# Output
Hello from your local environment!
```

### Build a JIT and Native Java application image
```
./mvnw package -DskipTests 

./mvnw native:compile -Pnative -DskipTests
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
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=quotes

./mvnw spring-boot:build-image  -DskipTests -Pnative -Dspring-boot.build-image.imageName=quotes-native
```

### Test the locally built images on the local machine
```shell
docker run --rm -p 8080:8083 quotes

docker run --rm -p 8080:8083 quotes-native
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
docker tag quotes gcr.io/${PROJECT_ID}/quotes
docker push gcr.io/${PROJECT_ID}/quotes

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
gcloud run deploy quotes \
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