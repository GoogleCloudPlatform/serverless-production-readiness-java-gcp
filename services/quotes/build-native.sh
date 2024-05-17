# build and push Native image to AR
docker rmi quotes-native:latest
docker rmi europe-docker.pkg.dev/spring-io/quotes-native/quotes-native:latest

./mvnw clean spring-boot:build-image -Pnative -DskipTests -Dspring-boot.build-image.imageName=quotes-native -Dmaven.test.skip

docker tag quotes-native:latest europe-docker.pkg.dev/spring-io/quotes-native/quotes-native:latest
docker push europe-docker.pkg.dev/spring-io/quotes-native/quotes-native:latest

