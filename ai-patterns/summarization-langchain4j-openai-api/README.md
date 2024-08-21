# Summarization Techniques - Langchain4J, OpenAI API support

__Use Case__:
Text summarization is the process of creating a shorter version of a text document while still preserving the most important information. 
This can be useful for a variety of purposes, such as quickly skimming a long document, getting the gist of an article, or sharing a summary with others.

__Which inference platforms are supported in this example__:
The OpenAI API is supported by multiple inference platforms.  This example explores the following, with their respective usage instructions for usage from a Langchain4J codebase:
* Open LLM deployed in GKE and exposed by [vLLM](https://docs.vllm.ai/en/latest/index.html) with an [OpenAI Compatible Server](https://docs.vllm.ai/en/latest/serving/openai_compatible_server.html)
* Gemini using the [OpenAI API](https://cloud.google.com/vertex-ai/generative-ai/docs/multimodal/call-gemini-using-openai-library)
If you are not using the OpenAI API, the recommended approach is to call Gemini using the Vertex SDK for Java, as illustrated in this [example](https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp/tree/main/ai-patterns/summarization-langchain4j)

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
# Running as a Model-as-a-Service (MaaS)
MODEL=meta/llama3-405b-instruct-maas;
MAX_TOKENS=1024;
BASE_URL=https://<location>-aiplatform.googleapis.com/v1beta1/projects/<project-id>/locations/<location>/endpoints/openapi

# Running with a hosted model in GKE
MODEL=meta-llama/Meta-Llama-3.1-8B-Instruct;
MAX_TOKENS=1024;
BASE_URL=:your ip>;
OPENAI_API_KEY=<your api key>>
```

## Setup Java ecosystem
In order to build JIT or Native Java app images, please set up Java (OpenJDK or GraalVM) with the associated Java 21 distributions.
A simple installer is available from [SDKMan](https://sdkman.io/install) for both JT and Native Java.

```shell
sdk install java 21.0.4-graal
```

## Clone the code:
```shell
git clone https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp.git
cd serverless-production-readiness-java-gcp/workshops/summarization-langchain4j
```

## Install the maven wrapper
The Maven Wrapper is an easy way to ensure a user of your Maven build has everything necessary to run your Maven build.

Run the command:
```shell
mvn wrapper:wrapper
```

Run the summarization tests from the command-line with the following command:
```shell
./mvnw test
```