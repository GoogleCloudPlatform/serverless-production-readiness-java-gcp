#!/bin/bash

# Get the list of Cloud Run services starting with "quotes"
services=$(gcloud run services list | grep us-central1 | awk '{print $2}')
echo "All Services"
echo $services

# Loop through each service
for service in $services; do
  if ! [[ "$service" =~ "pgo" || "$service" =~ "native" || "$service" =~ "crac" || "$service" =~ "cds" || "$service" =~ "leyden" ]]; then
    echo
    echo "Service: $service"
    gcloud alpha run services logs read $service --region us-central1 --limit=100 | grep "Started QuotesApplication" | cut -d ']'  -f 3-
    echo
  fi
done
