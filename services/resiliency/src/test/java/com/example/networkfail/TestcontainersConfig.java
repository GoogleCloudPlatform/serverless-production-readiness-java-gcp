package com.example.networkfail;

import java.io.IOException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

  @Bean
  @ServiceConnection
  public PostgreSQLContainer postgresSQLContainer() throws IOException {
    return new PostgreSQLContainer<>("postgres:15-alpine");
  }
}