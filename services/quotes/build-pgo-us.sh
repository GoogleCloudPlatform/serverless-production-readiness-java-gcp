# Build Docker image for Native Image with PGO

# how to build the PGO image
# (1) 
# instrument
# ./mvnw -Pnative -DbuildArgs=--pgo-instrument native:compile -f pom-3.2.x.xml 
# (2)./targ
# build image
#./mvnw -Pnative -DbuildArgs=--pgo=default.iprof native:compile -f pom-3.2.x.xml -DskipTests

docker rmi quotes-pgo:latest
docker rmi us-central1-docker.pkg.dev/spring-io/quotes-native/quotes-pgo:latest

docker build -t quotes-pgo -f Dockerfile-pgo .

# push to us-central1
docker tag quotes-pgo:latest us-central1-docker.pkg.dev/spring-io/quotes-native/quotes-pgo:latest
docker push us-central1-docker.pkg.dev/spring-io/quotes-native/quotes-pgo:latest
