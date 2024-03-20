package services;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import static java.time.Duration.ofSeconds;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

@Testcontainers
public class OllamaChatModelTest {
    static String MODEL_NAME = "gemma:7b"; // try "mistral", "llama2", "codellama", "phi" or "tinyllama"
    static String imageName = "tc-ollama-gemma-7b";

    @Container
    static OllamaContainer ollama = new OllamaContainer(
        DockerImageName.parse(imageName).asCompatibleSubstituteFor("ollama/ollama")
    );

    @Test
    void simple_example() {

        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(baseUrl())
                .modelName(MODEL_NAME)
                .build();
        long start = System.currentTimeMillis();
        String answer = model.generate("Provide 3 short bullet points explaining why Gemini is awesome");
        System.out.println(("Generate text for prompt in " + (System.currentTimeMillis()-start) + " ms"));

        System.out.println(answer);
    }

    @Test
    void summarize_test(){
        Path documentPath = toPath("The_Jungle_Book-Rudyard_Kipling-1894-public.txt");
        DocumentParser documentParser = new TextDocumentParser();
        Document document = FileSystemDocumentLoader.loadDocument(documentPath, documentParser);

        long start = System.currentTimeMillis();
        ChatLanguageModel model = OllamaChatModel.builder()
            .baseUrl(baseUrl())
            .modelName(MODEL_NAME)
            .timeout(ofSeconds(100))
            .build();

        System.out.println(("Build model and start container in " + (System.currentTimeMillis()-start) + " ms"));
        start = System.currentTimeMillis();
        String answer = model.generate(String.format("Summarize in no more than 2000 words the content of the book listed after the : %s", document.text()));
        System.out.println(("Summarize book in " + (System.currentTimeMillis()-start) + " ms"));

        System.out.println(answer);
    }

    @Test
    void summarize_test_direct(){
        long start = System.currentTimeMillis();
        ChatLanguageModel model = OllamaChatModel.builder()
            .baseUrl(baseUrl())
            .modelName(MODEL_NAME)
            .timeout(ofSeconds(120))
            .build();

        System.out.println(("Build model and start container in " + (System.currentTimeMillis()-start) + " ms"));
        start = System.currentTimeMillis();
        String answer = model.generate(String.format("Summarize in no more than 2000 words the content of the book The Jungle Book by Rudyard Kipling"));
        System.out.println(("Summarize book in " + (System.currentTimeMillis()-start) + " ms"));

        System.out.println(answer);
    }

    static Path toPath(String fileName) {
            return Paths.get("src","test","resources", fileName);
    }

    @Test
    void json_output_example() {

        ChatLanguageModel model = OllamaChatModel.builder()
                .baseUrl(baseUrl())
                .modelName(MODEL_NAME)
                .format("json")
                .build();

        String json = model.generate("Give me a JSON with 2 fields: name and age of a Rudyard Kipling, 100");

        System.out.println(json);
    }

    static String baseUrl() {
        return String.format("http://%s:%d", ollama.getHost(), ollama.getFirstMappedPort());
    }
}
