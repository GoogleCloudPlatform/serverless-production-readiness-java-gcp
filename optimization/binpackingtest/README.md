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