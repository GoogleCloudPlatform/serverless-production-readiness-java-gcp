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
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.DocumentEmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.model.Media;
import org.springframework.ai.vertexai.embedding.VertexAiEmbeddigConnectionDetails;
import org.springframework.ai.vertexai.embedding.multimodal.VertexAiMultimodalEmbeddingModel;
import org.springframework.ai.vertexai.embedding.multimodal.VertexAiMultimodalEmbeddingOptions;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmbeddingMultimodalController {
  @GetMapping("/ai/embedding/multimodal")
  public String embed() {
    VertexAiEmbeddigConnectionDetails connectionDetails =
        VertexAiEmbeddigConnectionDetails.builder()
            .withProjectId(System.getenv("VERTEX_AI_PROJECT_ID"))
        .withLocation(System.getenv("VERTEX_AI_LOCATION"))
        .build();

    // default multimodal embedding model multimodalembedding@001
    VertexAiMultimodalEmbeddingOptions options = VertexAiMultimodalEmbeddingOptions.builder()
        .withModel("multimodalembedding")
        .build();

    var embeddingModel = new VertexAiMultimodalEmbeddingModel(connectionDetails, options);

    Media imageMedial = new Media(MimeTypeUtils.IMAGE_PNG, new ClassPathResource("/Coffee.png"));
    Media videoMedial = new Media(new MimeType("video", "mp4"), new ClassPathResource("/Birds.mp4"));

    var document = new Document("Explain what do you see on this video?", List.of(imageMedial, videoMedial), Map.of());

    DocumentEmbeddingRequest embeddingRequest = new DocumentEmbeddingRequest(List.of(document),
        EmbeddingOptions.EMPTY);

    EmbeddingResponse embeddingResponse = embeddingModel.call(embeddingRequest);

    return "Multimodal embedding complete" + embeddingResponse.getResult().getOutput().toString();
  }
}
