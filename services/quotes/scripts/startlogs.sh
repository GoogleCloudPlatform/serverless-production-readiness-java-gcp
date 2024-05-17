#!/bin/bash

# Get the list of Cloud Run services starting with "quotes"
services=$(gcloud run services list | grep europe | awk '{print $2}')
echo "Services"
echo $services

# Loop through each service
for service in $services; do

  echo
  echo "Service: $service"
  gcloud alpha run services logs read $service --limit=200 | grep "Started QuotesApplication"
  echo
done
