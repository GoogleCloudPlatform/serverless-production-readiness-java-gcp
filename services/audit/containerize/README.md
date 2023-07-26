# Containerize Audit Service

Containerize the Audit service by executing one of the following commands from a command-line from the <repository-root>.

Check the build images and their sizes by running ```docker images | grep audit``` 

FatJAR
```shell
docker build -f ./containerize/Dockerfile-fatjar -t audit-fatjar .
```

Custom Layers
```shell
docker build -f ./containerize/Dockerfile-custom -t audit-custom .
```

Jlink
```shell
docker build -f ./containerize/Dockerfile-jlink -t audit-jlink .
```

Buildpacks
```shell
./mvnw spring-boot:build-image
```