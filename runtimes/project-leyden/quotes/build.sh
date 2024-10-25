# build and push JIT image to AR
docker rmi quotes:latest
docker rmi europe-docker.pkg.dev/spring-io/quotes/quotes:latest

./mvnw clean spring-boot:build-image -Dspring-boot.build-image.imageName=quotes -Dmaven.test.skip

docker tag quotes:latest europe-docker.pkg.dev/spring-io/quotes/quotes:latest
docker push europe-docker.pkg.dev/spring-io/quotes/quotes:latest

