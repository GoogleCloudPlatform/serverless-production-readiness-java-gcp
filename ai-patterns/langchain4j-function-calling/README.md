# Langchain4J Function Calling

Demonstrate `Function Calling` code using Gemini with Langchain4j

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

Function is registered as `@Tool`, which are Java methods the language model can use to call. 
Langchain4j greatly simplifies code you need to write to support function invocation.
It brokers the function invocation conversation for you.

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
ApplicationRunner applicationRunner() {
  return args -> {
    String userMessage = """
        Please use multi-turn conversation to answer the following questions:
        What is the status of my payment transactions 002, 001, 003?
        Please indicate the status for each transaction and return the results in JSON format.
        """;

    // test with VertexAI Gemini using REST API
    functionCallGeminiWithREST(userMessage);

    // test with VertexAI Gemini using gRPC
    functionCallGeminiWithGRPC(userMessage);
  };
}
```

The output would look something like:

```text
    What is the status of my payment transactions 002, 001, 003?
    Please indicate the status for each transaction and return the results in JSON format.
    ```json
    {
        "002": "approved",
        "001": "pending",
        "003": "rejected"
    }
    ```
```

## Related [Langchain4J](https://docs.langchain4j.dev/) documentation:
* [Langchain4J Google VertexAI Gemini](https://docs.langchain4j.dev/integrations/language-models/google-gemini) and [Function Calling](https://docs.langchain4j.dev/tutorials/tools)

## Native Java (GraalVM) Build
You can build this as a native executable.

First make sure that you are using GraalVM 21 JDK. For example, install the GraalVM 21 SDK with [SDKMan](https://sdkman.io/install) or from the [GraalVM site](https://www.graalvm.org/downloads/)

```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-21.0.2+13.1/Contents/Home
```

Then build:

```
./mvnw clean package -Pnative native:compile -DskipTests
```

Run the native executable:

```
./target/langchain4j-function-calling 
```

__Important note__: Please register runtime hints for the Native Java image
```shell
	public static class FunctionCallingRuntimeHints implements RuntimeHintsRegistrar {
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			// Register method for the Assistant AIService class
			var mcs = MemberCategory.values();
			hints.reflection().registerType(Langchain4JFunctionCallingApplication.Assistant.class, mcs);
			hints.proxies().registerJdkProxy(Langchain4JFunctionCallingApplication.Assistant.class);
			
			try {
				// Register all the classes and methods that are used through reflection
				// or dynamic proxy generation in LangChain4j, especially those
				// related to function calling.
				hints.reflection().registerType(FunctionCallingService.class, MemberCategory.values());

				// Corrected method registration
				hints.reflection().registerMethod(
						FunctionCallingService.class.getMethod("paymentStatus", String.class),
						ExecutableMode.INVOKE
				);
			} catch (NoSuchMethodException e) {
				// Handle the exception appropriately (e.g., log it)
				e.printStackTrace();
			}
        }
	}
```