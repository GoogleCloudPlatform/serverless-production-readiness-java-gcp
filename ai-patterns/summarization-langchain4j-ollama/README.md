# Summarization Techniques - Local deployment - Langchain4J, Ollama

__Use Case__:
Text summarization is the process of creating a shorter version of a text document while still preserving the most important information. 
This can be useful for a variety of purposes, such as quickly skimming a long document, getting the gist of an article, or sharing a summary with others.

__Summarization Techniques__:
In this sample, generative models are used to summarize text, using the following patterns:
* Prompt Stuffing method
* Refine method with Separate Chunks method
* Refine method with Overlapping Summary methods
* Map-reduce method with Separate Chunks method
* Map-reduce method with Overlapping Chunks method

__Environment__:
Please set the following environment variables before running this example:
```shell
export CHUNK_SIZE=<chunk size in characters> - default of 10000 provided if not set 
export OVERLAP_SIZE=<overlap window in characters> - default of 500 provided if not set
```

## Setup Java ecosystem
In order to build JIT or Native Java app images, please set up Java (OpenJDK or GraalVM) with the associated Java 21 distributions.
A simple installer is available from [SDKMan](https://sdkman.io/install) for both JT and Native Java.

```shell
sdk install java 21.0.3-graal
```

## Clone the code:
```shell
git clone https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp.git
cd serverless-production-readiness-java-gcp/workshops/summarization-langchain4j-ollama
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