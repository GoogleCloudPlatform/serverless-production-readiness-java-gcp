/*
 * Copyright 2025 Google LLC
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
package com.example.google;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

interface DogRepository extends ListCrudRepository<Dog, Integer> {

}

@SpringBootApplication
public class GoogleApplication {

  public static void main(String[] args) {
    SpringApplication.run(GoogleApplication.class, args);
  }

  @Bean
  McpSyncClient mcpSyncClient() {
    var mcp = McpClient
        .sync(HttpClientSseClientTransport.builder("http://localhost:8081").build())
        .build();
    mcp.initialize();
    return mcp;
  }
}

record Dog(@Id int id, String name, String owner, String description) {

}

@Controller
@ResponseBody
class AssistantController {

  private final ChatClient ai;

  private final Map<String, PromptChatMemoryAdvisor> advisors = new ConcurrentHashMap<>();

  AssistantController(JdbcClient db, DogRepository repository, McpSyncClient client,
      ChatClient.Builder ai, VectorStore vectorStore) {

    if (db.sql("select count(*) from vector_store").query(Integer.class).single().equals(0)) {
      repository.findAll().forEach(d -> {
        var dogument = new Document("id: %s, name: %s, description: %s".formatted(
            d.id(), d.name(), d.description()
        ));
        vectorStore.add(List.of(dogument));
      });
    }

    var system = """
        You are an AI powered assistant to help people adopt a dog from the adoption\s
        agency named Pooch Palace with locations in Mountain View, Seoul, Tokyo, Singapore, Paris,\s
        Mumbai, New Delhi, Barcelona, San Francisco, and London. Information about the dogs available\s
        will be presented below. If there is no information, then return a polite response suggesting we\s
        don't have any dogs available.
        """;
    this.ai = ai
        .defaultSystem(system)
        .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
        .defaultToolCallbacks(new SyncMcpToolCallbackProvider(client))
        .build();
  }

  @GetMapping("/{user}/inquire")
  String inquire(@PathVariable String user,
      @RequestParam String question) {
    var c = MessageWindowChatMemory.builder()
        .chatMemoryRepository(new InMemoryChatMemoryRepository()).build();
    var advisor = this.advisors
        .computeIfAbsent(user, _ -> PromptChatMemoryAdvisor.builder(c).build());
    // tbd changes in > m8
    return this.ai
        .prompt()
        .advisors(advisor)
        .user(question)
        .call()
        .content();
  }
}