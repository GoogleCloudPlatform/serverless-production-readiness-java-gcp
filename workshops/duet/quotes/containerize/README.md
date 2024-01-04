# Containerization Options for the Quotes Service

The following containerization options will be analyzed:
* Single-layer FAT Jar Docker build
* Multi-stage, multi-layer Docker build
* Multi-stage, multi-layer, JLink Docker Build with custom JRE
* Cloud-native buildpacks Docker build, no more Dockerfile

Containerize the Quotes service by using one of the four above mentioned optioins from the command-line, starting from the <repository-root>/services/quotes [`serverless-production-readiness-java-gcp/services/quotes`].

**NOTE**: Check the build images and their sizes by running ```docker images | grep quotes```

Pull the `Ubuntu:22.04` image and analyze it's layers:
```shell
docker pull ubuntu:22.04

docker history ubuntu:22.04

IMAGE          CREATED       CREATED BY                                      SIZE      COMMENT
e343402cadef   11 days ago   /bin/sh -c #(nop)  CMD ["/bin/bash"]            0B        
<missing>      11 days ago   /bin/sh -c #(nop) ADD file:f8594e26831508c31…   69.2MB    
<missing>      11 days ago   /bin/sh -c #(nop)  LABEL org.opencontainers.…   0B        
<missing>      11 days ago   /bin/sh -c #(nop)  LABEL org.opencontainers.…   0B        
<missing>      11 days ago   /bin/sh -c #(nop)  ARG LAUNCHPAD_BUILD_ARCH     0B        
<missing>      11 days ago   /bin/sh -c #(nop)  ARG RELEASE                  0B 
```

Pull the Eclipse Temurin image and analyze it's layers:
```shell
docker pull eclipse-temurin:17-jre

docker history eclipse-temurin:17-jre
 
# contributed by Java 17
IMAGE          CREATED       CREATED BY                                      SIZE      COMMENT
177809406af1   3 days ago    /bin/sh -c #(nop)  ENTRYPOINT ["/__cacert_en…   0B        
<missing>      3 days ago    /bin/sh -c #(nop) COPY file:8b8864b3e02a33a5…   1.18kB    
<missing>      3 days ago    /bin/sh -c echo Verifying install ...     &&…   0B        
<missing>      3 days ago    /bin/sh -c set -eux;     ARCH="$(dpkg --prin…   141MB     
<missing>      3 days ago    /bin/sh -c #(nop)  ENV JAVA_VERSION=jdk-17.0…   0B        
<missing>      3 days ago    /bin/sh -c apt-get update     && DEBIAN_FRON…   52.4MB    
<missing>      3 days ago    /bin/sh -c #(nop)  ENV LANG=en_US.UTF-8 LANG…   0B        
<missing>      3 days ago    /bin/sh -c #(nop)  ENV PATH=/opt/java/openjd…   0B        
<missing>      3 days ago    /bin/sh -c #(nop)  ENV JAVA_HOME=/opt/java/o…   0B     
# contributed by Ubuntu   
<missing>      11 days ago   /bin/sh -c #(nop)  CMD ["/bin/bash"]            0B        
<missing>      11 days ago   /bin/sh -c #(nop) ADD file:f8594e26831508c31…   69.2MB    
<missing>      11 days ago   /bin/sh -c #(nop)  LABEL org.opencontainers.…   0B        
<missing>      11 days ago   /bin/sh -c #(nop)  LABEL org.opencontainers.…   0B        
<missing>      11 days ago   /bin/sh -c #(nop)  ARG LAUNCHPAD_BUILD_ARCH     0B        
<missing>      11 days ago   /bin/sh -c #(nop)  ARG RELEASE                  0B      
```
## Single-layer FAT Jar Docker build

Use the Dockerfile [containerize/Dockerfile-fatjar]:
```shell
FROM eclipse-temurin:17-jre
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
ENTRYPOINT ["java","-jar","/application.jar"]
```

```shell
docker build -f ./containerize/Dockerfile-fatjar -t quotes-fatjar .

docker history quotes-fatjar
 
IMAGE          CREATED        CREATED BY                                      SIZE      COMMENT
# Contributed by the app
f604a49ff4d6   2 weeks ago    ENTRYPOINT ["java" "-jar" "/application.jar"]   0B        buildkit.dockerfile.v0
<missing>      2 weeks ago    COPY target/*.jar application.jar # buildkit    52.3MB    buildkit.dockerfile.v0
<missing>      2 weeks ago    ARG JAR_FILE=target/*.jar                       0B        buildkit.dockerfile.v0
# Contributed by Java 17 
<missing>      6 weeks ago    /bin/sh -c #(nop)  ENTRYPOINT ["/__cacert_en…   0B        
<missing>      6 weeks ago    /bin/sh -c #(nop) COPY file:8b8864b3e02a33a5…   1.18kB    
<missing>      6 weeks ago    /bin/sh -c echo Verifying install ...     &&…   0B        
<missing>      6 weeks ago    /bin/sh -c set -eux;     ARCH="$(dpkg --prin…   141MB     
<missing>      6 weeks ago    /bin/sh -c #(nop)  ENV JAVA_VERSION=jdk-17.0…   0B        
<missing>      6 weeks ago    /bin/sh -c apt-get update     && DEBIAN_FRON…   52.3MB    
<missing>      6 weeks ago    /bin/sh -c #(nop)  ENV LANG=en_US.UTF-8 LANG…   0B        
<missing>      6 weeks ago    /bin/sh -c #(nop)  ENV PATH=/opt/java/openjd…   0B        
<missing>      6 weeks ago    /bin/sh -c #(nop)  ENV JAVA_HOME=/opt/java/o…   0B        
# Contributed by Ubuntu
<missing>      2 months ago   /bin/sh -c #(nop)  CMD ["/bin/bash"]            0B        
<missing>      2 months ago   /bin/sh -c #(nop) ADD file:3fcf00866c55150f1…   69.2MB    
<missing>      2 months ago   /bin/sh -c #(nop)  LABEL org.opencontainers.…   0B        
<missing>      2 months ago   /bin/sh -c #(nop)  LABEL org.opencontainers.…   0B        
<missing>      2 months ago   /bin/sh -c #(nop)  ARG LAUNCHPAD_BUILD_ARCH     0B        
<missing>      2 months ago   /bin/sh -c #(nop)  ARG RELEASE                  0B        
```

## Multi-stage, multi-layer Docker build

Use the Dockerfile [containerize/Dockerfile-custom]:
```shell
FROM eclipse-temurin:17-jre as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:17-jre
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
```

```shell
docker build -f ./containerize/Dockerfile-custom -t quotes-custom .