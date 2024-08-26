package services;

import org.junit.Test;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

public class OllamaContainerTest {

    @Test
    public void withDefaultConfig() {
        try ( // container {
            OllamaContainer ollama = new OllamaContainer("ollama/ollama:0.1.26")
            // }
        ) {
            ollama.start();

            String version = given().baseUri(ollama.getEndpoint()).get("/api/version").jsonPath().get("version");
            assertThat(version).isEqualTo("0.1.26");
        }
    }

    @Test
    public void downloadModelAndCommitToImage() throws IOException, InterruptedException {
        String newImageName = "tc-ollama-gemma-2b";
        try (OllamaContainer ollama = new OllamaContainer("ollama/ollama:0.1.26")) {
            ollama.start();
            // pullModel {
            ollama.execInContainer("ollama", "pull", "gemma:2b");
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