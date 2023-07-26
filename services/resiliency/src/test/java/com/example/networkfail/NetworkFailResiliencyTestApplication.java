package com.example.networkfail;

import org.springframework.boot.SpringApplication;

public class NetworkFailResiliencyTestApplication {
  public static void main(String[] args) {
    SpringApplication
        .from(NetworkFailResiliencyApplication::main)
        .with(TestcontainersConfig.class)
        .run(args);
  }
}
