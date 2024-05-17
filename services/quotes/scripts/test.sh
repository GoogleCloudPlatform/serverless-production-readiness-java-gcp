#!/bin/bash

# Get the list of Cloud Run services starting with "quotes"
services=$(gcloud run services list | grep europe | awk '{print $4}')
echo "Services"
echo $services

# Loop through each service
for service in $services; do

  echo
  echo "Service: $service"
  # curl $service/start

  # Get the user time for the service
  echo "Start time:"
  date +"%T.%3N"
  
  #   user_time=$( { time curl $service/start ; } 2>&1 | grep user | cut -f2)
  # user_time=$( time curl $service/start )

  # Print the user time
  #   echo "User time: $user_time"
  curl $service/start
  echo
done
