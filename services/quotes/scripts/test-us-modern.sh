#!/bin/bash

# Get the list of Cloud Run services starting with "quotes"
services=$(gcloud run services list | grep us-central1 | awk '{print $4}')
echo "All Services"
echo $services

# Loop through each service
for service in $services; do
      if [[ "$service" =~ "pgo" || "$service" =~ "native" || "$service" =~ "crac" || "$service" =~ "cds" || "$service" =~ "leyden" ]]; then
        echo
      echo "Service: $service"

      # Get the user time for the service
      echo "Start time:"
      date +"%T.%3N"

      curl $service/start
  fi
  # curl $service/start
  echo
done
