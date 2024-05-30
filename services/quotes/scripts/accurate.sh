# start the accurate-wait.sh script first

echo "Start time:"
date + "%T.%3N"

java -jar target/quotes-1.0.0.jar

echo "End time:"
date + "%T.%3N"