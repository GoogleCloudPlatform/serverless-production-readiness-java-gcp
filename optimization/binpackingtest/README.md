# Test a BinPacking algorithm

Originally explained in detail [here](https://developers.google.com/optimization/pack/bin_packing)

To build the code, you need to set up the same pre-requisites as outlined in the [Repository Root README file](../../README.md)

## Build
Build the app:
```shell
# build the app
./mvnw package

# build the container image 
# note that the build is machine architecture specific
# At this time, Cloud Run uses an AMD64 architecture
./mvnw spring-boot:build-image
```

To run the app locally, you can use:
* random generated data for bin packing
* file based bin packing - files are available for 100, 500, 750, 1000, 10000 items in the `src/main/resources/data`

Run or debug as a stand-alone app in your IDE:
* open the BinPacking.java class and run the main() method

## Start
Start the app locally from the CLI:
```shell
./mvnw spring-boot:run
```

## Test
Test from the CLI using HTTPie:
```shell
# Test against the RANDOM endpoint
# defaults values: limit items to 100, capacity of the bin to 100, weight random between 1:27
http :8080/random

# values: limit items to 100, capacity of the bin to 100, weight random defaulted to between 1:27  
http ":8080/random?limit=100&bin=100"

# values: limit items to 100, capacity of the bin to 100, weight random generated to between 1:20
http ":8080/random?limit=100&bin=100&itemmaxweight=20"


# Test against the FILE endpoint
# defaults values: limit items to 100, capacity of the bin to 100, weight random between 1:27
http :8080/file

# values: limit items to 100, capacity of the bin to 100, weight random defaulted to between 1:27  
http ":8080/file?limit=100&bin=100"

# values: limit items to 100, capacity of the bin to 100, weight random generated to between 1:20
# note: files generated for 100, 500, 750, 1000, 10000
http ":8080/file?limit=100&bin=100&itemmaxweight=20"
```

## Deploy to Cloud Run
Retrieve the Project ID, as it will be required for the next GCP operations
```shell
export PROJECT_ID=$(gcloud config get-value project)
echo $PROJECT_ID
```

Tag the image and store it in GCR
```shell
docker tag binpackingtest:0.0.1-SNAPSHOT gcr.io/${PROJECT_ID}/binpackingtest:0.0.1-SNAPSHOT

docker push gcr.io/${PROJECT_ID}/binpackingtest:0.0.1-SNAPSHOT
```

Deploy to CloudRun 
```shell
gcloud run deploy binpackingtest \
     --image gcr.io/${PROJECT_ID}/binpackingtest:0.0.1-SNAPSHOT \
     --region us-central1 \
     --memory 4Gi --cpu=1 \
     --allow-unauthenticated
     
gcloud run deploy binpackingtest-tuned \
     --image gcr.io/${PROJECT_ID}/binpackingtest:0.0.1-SNAPSHOT \
     --region us-central1 \
     --memory 8Gi --cpu=2 \
     --set-env-vars=JAVA_TOOL_OPTIONS='-XX:+UseG1GC -XX:MaxRAMPercentage=75 -XX:ActiveProcessorCount=2 -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xss256k'\
     --allow-unauthenticated     
```

Test with the URL returned by the deployment:
```shell
URL=$(gcloud run services describe binpackingtest --region us-central1 --format='value(status.URL)')
# or
URL=$(gcloud run services describe binpackingtest-tuned --region us-central1 --format='value(status.URL)')

curl -H "Authorization: Bearer $(gcloud auth print-identity-token)" $URL/random
curl -H "Authorization: Bearer $(gcloud auth print-identity-token)" $URL/file
```