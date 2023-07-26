# Containerize Quotes Service

Containerize the Quotes service by executing one of the following commands from a command-line from the <repository-root>.

Check the build images and their sizes by running ```docker images | grep quotes``` 

FatJAR
```shell
docker build -f ./containerize/Dockerfile-fatjar -t quotes-fatjar .
```

Custom Layers
```shell
docker build -f ./containerize/Dockerfile-custom -t quotes-custom .
```

Jlink
```shell
docker build -f ./containerize/Dockerfile-jlink -t quotes-jlink .
```

Buildpacks
```shell
./mvnw spring-boot:build-image
```