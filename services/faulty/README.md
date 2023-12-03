# Faulty Service - JIT and Native Java Build & Deployment to Cloud Run

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
curl localhost:8087
```

### Build a JVM and Native Java application image
```
./mvnw clean package 

./mvnw native:compile -Pnative
```

### Build a JIT and Native Java Docker Image with Buildpacks
```
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=faulty

./mvnw spring-boot:build-image  -DskipTests -Pnative -Dspring-boot.build-image.imageName=faulty-native
```

### Test the locally built images on the local machine
```shell
docker run --rm -p 8080:8087 faulty

docker run --rm -p 8080:8087 faulty-native
```