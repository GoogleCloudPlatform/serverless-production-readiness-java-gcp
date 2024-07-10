# build and push JIT image to AR
docker rmi quotes-leyden-premain:latest
docker rmi europe-docker.pkg.dev/spring-io/quotes/quotes-leyden-premain:latest

./mvnw clean package -DskipTests
docker build -t quotes-leyden-premain -f Dockerfile-leyden .

# push to europe
docker tag quotes-leyden-premain:latest europe-docker.pkg.dev/spring-io/quotes/quotes-leyden-premain:latest
docker push europe-docker.pkg.dev/spring-io/quotes/quotes-leyden-premain:latest