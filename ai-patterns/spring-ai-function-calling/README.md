# Spring AI Function Calling

Note: This class has originally been started by Christian Tzolov
Repo: github.com:tzolov/spring-ai-function-calling-portability

Demonstrate `Function Calling` code using Gemini with Spring AI

__Use Case__: Suppose we want the AI model to respond with information that it does not have.
For example the status of your recent payment transactions.
Users can ask questions about current status for certain payment transactions and use function calling to answer them.

__Environment__:
Please set the following environment variables before running this example:
```shell
export VERTEX_AI_GEMINI_PROJECT_ID=<your project id>
export VERTEX_AI_GEMINI_LOCATION=<region, ex us-central1>
export VERTEX_AI_GEMINI_MODEL=<the model in use, ex.gemini-1.5-flash-001>
```

For example, let's consider a sample dataset and a function that retrieves the payment status given a transaction:

```java
record Transaction(String id) {
}

record Status(String name) {
}

private static final Map<Transaction, Status> DATASET =
	Map.of(
		new Transaction("001"), new Status("pending"),
		new Transaction("002"), new Status("approved"),
		new Transaction("003"), new Status("rejected"));

@Bean
@Description("Get the status of a payment transaction")
public Function<Transaction, Status> paymentStatus() {
	return transaction -> DATASET.get(transaction);
}
```

Function is registered as `@Bean` and uses the `@Description` annotation to define function description.
Spring AI greatly simplifies code you need to write to support function invocation.
It brokers the function invocation conversation for you.
You simply provide your function definition as a `@Bean` and then provide the bean name of the function in your prompt options.

Lets add the boot starters for 4 AI Models that support function calling:

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.ai</groupId>
	<artifactId>spring-ai-vertex-ai-gemini-spring-boot-starter</artifactId>
</dependency>
```

and configure them in `application.properties`:

```
# Google VertexAI Gemini
spring.ai.vertex.ai.gemini.project-id=${VERTEX_AI_GEMINI_PROJECT_ID}
spring.ai.vertex.ai.gemini.location=${VERTEX_AI_GEMINI_LOCATION}
spring.ai.vertex.ai.gemini.chat.options.model=${VERTEX_AI_GEMINI_MODEL}
spring.ai.vertex.ai.gemini.transport=grpc
spring.ai.vertex.ai.gemini.chat.options.functions=paymentStatus
spring.threads.virtual.enabled=true
```

Now you can test them with the same prompt:

```java
@Bean
ApplicationRunner applicationRunner(VertexAiGeminiChatClient vertexAiGemini) {

  return args -> {
    String prompt = """
        Please use multi-turn invocation to answer the following question:
        What is the status of my payment transactions 002, 001 and 003?
        Please indicate the status for each transaction and return the results in JSON format
        """;
}
```

The output would look something like:

```
VERTEX_AI_GEMINI: ```json
{
 "002": "approved",
 "001": "pending",
 "003": "rejected"
}
```

## Related [Spring AI](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/) documentation:
* [Spring AI Google VertexAI Gemini](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/api/clients/vertexai-gemini-chat.html) and [Function Calling](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/api/clients/functions/vertexai-gemini-chat-functions.html)

## Native (GraalVM) Build

You can build this as a native executable.

First make sure that you are using GraalVM 21 JDK. For example, install the GraalVM 21 SDK with [SDKMan](https://sdkman.io/install) or from the [GraalVM site](https://www.graalvm.org/downloads/)

```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-21.0.2+13.1/Contents/Home
```

Then build:

```
./mvnw clean package -Pnative native:compile
```

Run the native executable:

```
./target/function-calling
```
