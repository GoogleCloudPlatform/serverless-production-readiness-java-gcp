# build and push JIT image to AR
docker rmi quotes-cds:latest
docker rmi us-central1-docker.pkg.dev/spring-io/quotes/quotes-cds:latest

./mvnw clean package -DskipTests
docker build -t quotes-cds -f Dockerfile .

# push to us-central1
docker tag quotes-cds:latest us-central1-docker.pkg.dev/spring-io/quotes/quotes-cds:latest
docker push us-central1-docker.pkg.dev/spring-io/quotes/quotes-cds:latest
