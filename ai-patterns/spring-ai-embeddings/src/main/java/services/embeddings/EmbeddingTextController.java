/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package services.embeddings;

import java.util.List;
import java.util.Map;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vertexai.embedding.VertexAiEmbeddigConnectionDetails;
import org.springframework.ai.vertexai.embedding.text.VertexAiTextEmbeddingModel;
import org.springframework.ai.vertexai.embedding.text.VertexAiTextEmbeddingOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmbeddingTextController {

  @GetMapping("/ai/embedding")
  public Map embed(@RequestParam(value = "message", defaultValue = "Generate a default embedding") String message) {

    VertexAiEmbeddigConnectionDetails connectionDetails =
        VertexAiEmbeddigConnectionDetails.builder()
            .withProjectId(System.getenv("VERTEX_AI_PROJECT_ID"))
            .withLocation(System.getenv("VERTEX_AI_LOCATION"))
            .build();

    // Default embedding model: text-embedding-004
    VertexAiTextEmbeddingOptions options = VertexAiTextEmbeddingOptions.builder()
        .withModel(VertexAiTextEmbeddingOptions.DEFAULT_MODEL_NAME)
        .build();

    var embeddingModel = new VertexAiTextEmbeddingModel(connectionDetails, options);

    EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(List.of(message));
    return Map.of("embedding", embeddingResponse);
  }
}
