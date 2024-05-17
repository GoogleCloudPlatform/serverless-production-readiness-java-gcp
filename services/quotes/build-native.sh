# build and push Native image to Artifact Registry
docker rmi quotes-native:latest
docker rmi europe-docker.pkg.dev/spring-io/quotes-native/quotes-native:latest

./mvnw -f pom-2.3.x.xml clean spring-boot:build-image -Pnative -DskipTests -Dspring-boot.build-image.imageName=quotes-native -Dmaven.test.skip

# push to europe-west1
docker tag quotes-native:latest europe-docker.pkg.dev/spring-io/quotes-native/quotes-native:latest
docker push europe-docker.pkg.dev/spring-io/quotes-native/quotes-native:latest