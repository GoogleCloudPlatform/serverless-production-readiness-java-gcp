# Summarization Techniques - SpringAI, VertexAI, Gemini

## This document is being updated at this time, treat it is WIP!

This lab can be executed directly in a Cloud Workstation, Cloudshell or your environment of choice. 
[![Open in Cloud Shell](https://gstatic.com/cloudssh/images/open-btn.svg)](https://shell.cloud.google.com/cloudshell/editor?cloudshell_git_repo=https://github.com/GoogleCloudPlatform/serverless-photosharing-workshop.git)

## Setup Java ecosystem
In order to build JIT or Native Java app images, please set up Java (OpenJDK or GraalVM) with the associated Java 21 distributions.
A simple installer is available from [SDKMan](https://sdkman.io/install) for both JT and Native Java.

```shell
sdk install java 21.0.3-graal
```

## Clone the code:
```shell
git clone https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp.git
cd serverless-production-readiness-java-gcp/workshops/summarization-springai
```

## Install the maven wrapper
The Maven Wrapper is an easy way to ensure a user of your Maven build has everything necessary to run your Maven build.

Run the command:
```shell
mvn wrapper:wrapper
```

Run the summarization tests from the command-line with the following command:
```shell
./mvnw verify
```