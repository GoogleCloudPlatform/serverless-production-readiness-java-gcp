# build and push JIT image to AR
docker rmi quotes:latest
docker rmi us-central1-docker.pkg.dev/spring-io/quotes/quotes:latest

./mvnw clean spring-boot:build-image -Dspring-boot.build-image.imageName=quotes -Dmaven.test.skip

docker tag quotes:latest us-central1-docker.pkg.dev/spring-io/quotes/quotes:latest
docker push us-central1-docker.pkg.dev/spring-io/quotes/quotes:latest

