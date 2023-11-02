## Reference Service CRaC Build & Deploy to Cloud Run

This repo leverages checkpoint/restore scripts originally provided Seb Deleuze (sdeleuze/spring-boot-crac-demo)
It is intended to demonstrate Spring Boot 3.2+ CRaC support by creating a ready to restore container image.

Please note that, while you can build the checkpointed image on both Intel and Apple M* series, in order to deploy to Cloud Run you have to build in a similar architecture as the one you would be running on. See the [Cloud Run Container Runtime Contract](https://cloud.google.com/run/docs/container-contract)

**Warning**: for real projects make sure to not leak sensitive data in CRaC files since they contain a snapshot of the memory of the running JVM instance.

### Step 1: Checkpoint

From within the `reference-crac` folder, you have the choice to run either [on demand checkpoint/restore of a running application](https://docs.spring.io/spring-framework/reference/6.1/integration/checkpoint-restore.html#_on_demand_checkpointrestore_of_a_running_application) with:
```
./checkpoint.sh
```

Or to run an [automatic checkpoint/restore at startup](https://docs.spring.io/spring-framework/reference/6.1/integration/checkpoint-restore.html#_on_demand_checkpointrestore_of_a_running_application) with:
```
./checkpointOnRefresh.sh
```

### Step 2: Restore
Restore the application with:
```
./restore.sh
```

## Deploy CRaC Image
Let's build the checkpointed CRaC image and deploy it to Cloud Run.
```shell
# first, build the image if you have not done so before
./checkpoint.sh

# tag the image
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   $PROJECT_ID
docker tag reference-crac:checkpoint gcr.io/${PROJECT_ID}/reference-crac

# push the image to GCR
docker push gcr.io/${PROJECT_ID}/reference-crac
```

Deploy the image to Cloud Run and test it
```shell
# get the PROJECT_ID
export PROJECT_ID=$(gcloud config list --format 'value(core.project)')
echo   $PROJECT_ID

gcloud run deploy reference-crac  \
    --image=gcr.io/${PROJECT_ID}/reference-crac  \
    --execution-environment=gen2  \
    --allow-unauthenticated \
    --region=us-central1 \
    --memory 2Gi --cpu 2 

# observe the deploy output
...
  ✓ Deploying... Done.                                                                                                                                
  ✓ Creating Revision...                                                                                                                            
  ✓ Routing traffic...                                                                                                                              
  ✓ Setting IAM Policy...                                                                                                                           
Done.                                                                                                                                               
Service [reference-crac] revision [spring-crac-00003-his] has been deployed and is serving 100 percent of traffic.
Service URL: https://reference-crac-....XYZ   
```

Run a test request:
```shell
# use your deployment URL
curl https://reference-crac-....XYZ/start

# Note: if you run your service as authenticated, use 
curl -i  -H "Authorization: Bearer $(gcloud auth print-identity-token)" https://reference-crac-....XYZ/start
```