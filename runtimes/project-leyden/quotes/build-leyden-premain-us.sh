# build and push JIT image to AR
docker rmi quotes-leyden-premain:latest
docker rmi us-central1-docker.pkg.dev/spring-io/quotes/quotes-leyden-premain:latest

./mvnw clean package -DskipTests
docker build -t quotes-leyden-premain -f Dockerfile-leyden .

# push to us-central1
docker tag quotes-leyden-premain:latest us-central1-docker.pkg.dev/spring-io/quotes/quotes-leyden-premain:latest
docker push us-central1-docker.pkg.dev/spring-io/quotes/quotes-leyden-premain:latest