package services;

import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

public class OllamaContainerTest {

    static String IMAGE_NAME_PREFIX = "tc-ollama-gemma-";
    static String GEMMA2B = "2b";
    static String GEMMA7B = "7b";
    static String OLLAMA_CONTAINER_VERSION="0.1.32";

    @Test
    public void withDefaultConfig() {
        try ( // container {
            OllamaContainer ollama = new OllamaContainer("ollama/ollama:" + OLLAMA_CONTAINER_VERSION);
            // }
        ) {
            ollama.start();

            String version = given().baseUri(ollama.getEndpoint()).get("/api/version").jsonPath().get("version");
            assertThat(version).isEqualTo(OLLAMA_CONTAINER_VERSION);
        }
    }

    @Ignore
    @Test
    public void downloadModelAndCommitToImage2B() throws IOException, InterruptedException {
        String newImageName = IMAGE_NAME_PREFIX + GEMMA2B;
        createContainer(newImageName, GEMMA2B);
    }

    @Ignore
    @Test
    public void downloadModelAndCommitToImage7B() throws IOException, InterruptedException {
        String newImageName = IMAGE_NAME_PREFIX + GEMMA7B;
        createContainer(newImageName, GEMMA7B);
    }

    private void createContainer(String newImageName, String model) throws IOException, InterruptedException {
        try (OllamaContainer ollama = new OllamaContainer("ollama/ollama:" + OLLAMA_CONTAINER_VERSION)) {
            ollama.start();
            // pullModel {
            ollama.execInContainer("ollama", "pull", "gemma:"+model);
            // }

            String modelName = given()
                .baseUri(ollama.getEndpoint())
                .get("/api/tags")
                .jsonPath()
                .getString("models[0].name");
            assertThat(modelName).contains("gemma");
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
            assertThat(modelName).contains("gemma");
        }
    }
}