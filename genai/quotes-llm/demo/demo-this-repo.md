# Add functionality to an application using Gemini CodeAssist 

# Gemini CLI

Add custom commands to Gemini CLI: [Custom Commands - Explain, Plan, Implement, Deploy](https://github.com/GoogleCloudPlatform/serverless-production-readiness-java-gcp/blob/main/genai/gemini-cli-extensions/README.md)

### Task: add an getByBook endpoint to the app and all dependent functionality
```
/explain please provide a detailed analysis of the current project
```

```
/explain architecture of the current project
```

```
/explain analyze all Java files in @src and provide a brief overview of each class
```

```
/plan:new let's plan the addition of a getByBook endpoint to the application, with the current Java code available in @src. Add all required functionality in downstream classes. when returning the results, handle and log any exceptions properly
```

```
/plan:refine @plans/add-getByBook-endpoint.md add also tests for the repository, not only for the QuoteController. Add also tests for the QuoteEndpoint
```

```
/plan:impl Implement the plan at @ /plan:refine @plans/add-getByBook-endpoint.md
```

Occasionally:
```
I do not see you updating the @plans/add-getByBook-endpoint.md file to track your progress. revisit this
```

```
/deploy:project generate the Dockerfile and step-by-step deployment instructions for this maven project to Google Cloud Run serverless
```

# Gemini CodeAssist 

### Task: add an getByBook endpoint to the app and all dependent functionality

```shell
./mvnw package spring-boot:run
```

```Please provide a brief explanation of each Java file in the folder quotes-llm
```

```
I want to get details about the QuotesApplication; please provide a detailed overview of the QuotesApplication
```
```
Please perform a detailed code review of the QuoteEndpoint
```
```
Please recommend code improvements to the QuoteEndpoint class
```

```
Which types of test should I be writing for the QuoteEndpoint
```

```
Please answer briefly whether I should add tests for network failures?
```

```
Answer as a Software Engineer with expertise in Java. Create a test for the QuoteEndpoint for a method quoteByBook which responds to the UI and retrieves a quote from the book The Road
```

**Use commands inlined in the editor as comments**
```
// generate a getByBook method which retrieves a quote by book name
```

```
// generate a unit test for the getByBook method in the QuoteService; create a Quote in the QuoteService first then test the getByBook method against the new Quote
```

```
// generate a getByBook method which retrieves a quote by book name
```

```
// generate a findByBook method which retrieves a quote by book name; use the nativeQuery syntax
```

```
@QuoteEndpoint refine the quoteByBook method in QuoteEndpoint, handle the error codes from the QuotesService and return the error codes to the UI
```

```
Answer as a Software Engineer with expertise in Java. Create an endpoint for the QuoteEndpoint for a method quoteByBookandAuthor which responds to the UI and retrieves a quote using the book name and book author; implement the endpoint, and downstream service and repository methods
```
