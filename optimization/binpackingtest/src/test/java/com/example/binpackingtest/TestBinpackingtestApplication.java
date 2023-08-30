package com.example.binpackingtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestBinpackingtestApplication {

	public static void main(String[] args) {
		SpringApplication.from(BinpackingtestApplication::main).with(TestBinpackingtestApplication.class).run(args);
	}

}
