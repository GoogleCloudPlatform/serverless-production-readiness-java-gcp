#!/bin/bash

# exit if a command returns a non-zero exit code and also print the commands and their args as they are executed.
set -e -x

# Assuming from the root directory of the project
ROOT_DIR=`dirname $(realpath $0)`/../

cd $ROOT_DIR/services/audit
gcloud builds submit .  --machine-type E2-HIGHCPU-32 --config cloudbuild-native.yaml 

cd $ROOT_DIR/services/faulty
gcloud builds submit .  --machine-type E2-HIGHCPU-32 --config cloudbuild.yaml --substitutions=_TYPE=native

cd $ROOT_DIR/services/bff
gcloud builds submit .  --machine-type E2-HIGHCPU-32 --config cloudbuild.yaml --substitutions=_TYPE=native

cd $ROOT_DIR/services/reference
gcloud builds submit .  --machine-type E2-HIGHCPU-32 --config cloudbuild.yaml --substitutions=_TYPE=native

cd $ROOT_DIR/services/quotes
gcloud builds submit .  --machine-type E2-HIGHCPU-32 --config cloudbuild-native.yaml
