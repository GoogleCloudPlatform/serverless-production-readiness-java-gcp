# Langchain4J Function Calling

Demonstrate `Function Calling` code using Gemini with Langchain4j

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
Langchain4j greatly simplifies code you need to write to support function invocation.
It brokers the function invocation conversation for you.
You simply provide your function definition as a `@Bean` and then provide the bean name of the function in your prompt options.

Lets add the boot starters for 4 AI Models that support function calling:

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-starter</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-vertex-ai-gemini</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-core</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

and configure them in `application.properties`:

```
# Google VertexAI Gemini
langchain4j.gemini.project-id=${VERTEX_AI_GEMINI_PROJECT_ID}
langchain4j.gemini.location=${VERTEX_AI_GEMINI_LOCATION}
langchain4j.gemini.chat.options.model=${VERTEX_AI_GEMINI_MODEL}
langchain4j.gemini.transport=rest
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
./mvnw clean package -Pnative native:compile
```

Run the native executable:

```
./target/function-calling
```
