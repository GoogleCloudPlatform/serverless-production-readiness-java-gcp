# build and push JIT image to AR
docker rmi quotes-cds:latest
docker rmi europe-docker.pkg.dev/spring-io/quotes/quotes-cds:latest

./mvnw clean package -DskipTests
docker build -t quotes-cds .

docker tag quotes:latest europe-docker.pkg.dev/spring-io/quotes/quotes-cds:latest
docker push europe-docker.pkg.dev/spring-io/quotes/quotes-cds:latest

