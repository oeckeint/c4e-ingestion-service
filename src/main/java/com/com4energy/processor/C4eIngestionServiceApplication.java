package com.com4energy.processor;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableRabbit
@SpringBootApplication
public class C4eIngestionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(C4eIngestionServiceApplication.class, args);
	}

}
