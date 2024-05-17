# Build Docker image for Native Image with PGO

docker rmi quotes-pgo:latest
docker rmi europe-docker.pkg.dev/spring-io/quotes-native/quotes-pgo:latest

docker build -t quotes-pgo -f Dockerfile-pgo .

docker tag quotes-pgo:latest europe-docker.pkg.dev/spring-io/quotes-native/quotes-pgo:latest
docker push europe-docker.pkg.dev/spring-io/quotes-native/quotes-pgo:latest
