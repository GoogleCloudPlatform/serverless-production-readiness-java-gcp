# build and push Native image to Artifact Registry
docker rmi quotes-native:latest
docker rmi us-central1-docker.pkg.dev/spring-io/quotes-native/quotes-native:latest

./mvnw -f pom-3.2.x.xml clean spring-boot:build-image -Pnative -DskipTests -Dspring-boot.build-image.imageName=quotes-native -Dmaven.test.skip

# push to us-central1
docker tag quotes-native:latest us-central1-docker.pkg.dev/spring-io/quotes-native/quotes-native:latest
docker push us-central1-docker.pkg.dev/spring-io/quotes-native/quotes-native:latest
