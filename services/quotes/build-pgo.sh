# Build Docker image for Native Image with PGO

# how to build the PGO image
# (1) 
# instrument
# ./mvnw -Pnative -DbuildArgs=--pgo-instrument native:compile -f pom-3.2.x.xml 
# (2)./targ
# build image
#./mvnw -Pnative -DbuildArgs=--pgo=default.iprof native:compile -f pom-3.2.x.xml -DskipTests

docker rmi quotes-pgo:latest
docker rmi europe-docker.pkg.dev/spring-io/quotes-native/quotes-pgo:latest

docker build -t quotes-pgo -f Dockerfile-pgo .

# push to europe
docker tag quotes-pgo:latest europe-docker.pkg.dev/spring-io/quotes-native/quotes-pgo:latest
docker push europe-docker.pkg.dev/spring-io/quotes-native/quotes-pgo:latest
