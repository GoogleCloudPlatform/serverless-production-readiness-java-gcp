# Spring AI Function Calling

Note: This class has been originally started by Christian Tzolov

Repo: github.com:tzolov/spring-ai-function-calling-portability

Demonstrate `Function Calling` code using Gemini with Spring AI

Use Case: Suppose we want the AI model to respond with information that it does not have.
For example the status of your recent payment transactions.
Users can ask questions about current status for certain payment transactions and use function calling to answer them.

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
spring.ai.vertex.ai.gemini.chat.options.model=gemini-pro
spring.ai.vertex.ai.gemini.chat.options..functions=paymentStatus
```

Now you can test them with the same prompt:

```java
@Bean
ApplicationRunner applicationRunner(VertexAiGeminiChatClient vertexAiGemini) {

	return args -> {

		String prompt = "What is the status of my payment transaction 003?";
		System.out.println("VERTEX_AI_GEMINI: " + vertexAiGemini.call(prompt));
	};
}
```

The output would look something like:

```
VERTEX_AI_GEMINI: Your transaction has been rejected.
```



## Related [Spring AI](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/) documentation:
* [Spring AI Google VertexAI Gemini](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/api/clients/vertexai-gemini-chat.html) and [Function Calling](https://docs.spring.io/spring-ai/reference/0.8-SNAPSHOT/api/clients/functions/vertexai-gemini-chat-functions.html)

## Native (GraalVM) Build

You can build this as a native executable.

First maker sure you are using GraalVM 21 JDK. For example:

```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-21.0.2+13.1/Contents/Home
```

Then build:

```
./mvnw clean install -Pnative native:compile
```

Run the native executable:

```
./target/function-calling
```
