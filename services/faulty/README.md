# Optimize Serverless Apps In Google Cloud - Faulty Service

### Create a Spring Boot Application

```
# Note: subject to change!
git clone https://github.com/ddobrin/serverless-production-readiness-java-gcp.git

# Note: subject to change!
cd prod/faulty-service
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
curl localhost:8087
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