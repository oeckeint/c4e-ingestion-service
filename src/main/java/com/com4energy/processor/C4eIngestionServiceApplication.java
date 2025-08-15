package com.com4energy.processor;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.com4energy.processor.config.properties.FileScannerProperties;
import com.com4energy.processor.config.properties.FileUploadProperties;

@EnableConfigurationProperties({
		FileScannerProperties.class,
		FileUploadProperties.class})
@EnableRabbit
@EnableScheduling
@SpringBootApplication
public class C4eIngestionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(C4eIngestionServiceApplication.class, args);
	}

}
