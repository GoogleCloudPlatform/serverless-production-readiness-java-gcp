package com.example.networkfail;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/services")
public class NetworkFailController {
  private final ServerlessServicesRepository serverlessServicesRepository;

  public NetworkFailController(ServerlessServicesRepository serverlessServicesRepository) {
    this.serverlessServicesRepository = serverlessServicesRepository;
  }

  @GetMapping
  String findAll(){
    return this.serverlessServicesRepository.findAll().collectList().toString();
  }
}
