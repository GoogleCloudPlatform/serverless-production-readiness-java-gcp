# build and push JIT image to AR
docker rmi quotes-leyden-aot-premain:latest
docker rmi us-central1-docker.pkg.dev/spring-io/quotes/quotes-leyden-aot-premain:latest

./mvnw clean package -DskipTests
docker build -t quotes-leyden-aot-premain -f Dockerfile-leyden-aot .

# push to us-central1
docker tag quotes-leyden-aot-premain:latest us-central1-docker.pkg.dev/spring-io/quotes/quotes-leyden-aot-premain:latest
docker push us-central1-docker.pkg.dev/spring-io/quotes/quotes-leyden-aot-premain:latest