# start the accurate-wait.sh script first

start_time = date +"%T.%3N"

java -jar target/quotes-1.0.0.jar
end_time = date +"%T.%3N"

echo "Start time is $start_time"
echo "End time is $end_time"