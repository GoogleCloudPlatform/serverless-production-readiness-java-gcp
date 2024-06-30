package services;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.Test;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

public class OllamaContainerTest {

    //    public static final String GEMMA_7_B = "gemma:7b";
    public static final String MODEL = "gemma2";
    public static final String MODEL_VALIDATION = "gemma2";

    public static final String OLLAMA = "ollama/ollama:0.1.48";
    public static final String OLLAMA_VERSION = "0.1.48";

    //    public static final String MODEL_IMAGE_NAME = "tc-ollama-gemma-7b";
    public static final String MODEL_IMAGE_NAME = "tc-ollama-gemma2";

    @Test
    public void withDefaultConfig() {
        try ( // container {
            OllamaContainer ollama = new OllamaContainer(OLLAMA)
            // }
        ) {
            ollama.start();

            String version = given().baseUri(ollama.getEndpoint()).get("/api/version").jsonPath().get("version");
            assertThat(version).isEqualTo(OLLAMA_VERSION);
        }
    }

    @Test
    public void downloadModelAndCommitToImage() throws IOException, InterruptedException {
        String newImageName = MODEL_IMAGE_NAME;
        try (OllamaContainer ollama = new OllamaContainer(OLLAMA)) {
            ollama.start();
            // pullModel {
            ollama.execInContainer("ollama", "pull", MODEL);
            // }

            String modelName = given()
                .baseUri(ollama.getEndpoint())
                .get("/api/tags")
                .jsonPath()
                .getString("models[0].name");
            assertThat(modelName).contains(MODEL_VALIDATION);
            // commitToImage {
            ollama.commitToImage(newImageName);
            // }
        }
        try (
            // substitute {
            OllamaContainer ollama = new OllamaContainer(
                DockerImageName.parse(newImageName).asCompatibleSubstituteFor("ollama/ollama")
            )
            // }
        ) {
            ollama.start();
            String modelName = given()
                .baseUri(ollama.getEndpoint())
                .get("/api/tags")
                .jsonPath()
                .getString("models[0].name");
            assertThat(modelName).contains(MODEL_VALIDATION);
        }
    }
}