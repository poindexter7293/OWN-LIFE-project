package com.ownlife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan
@EnableConfigurationProperties
@SpringBootApplication
@EnableScheduling
public class OwnlifeApplication {

	public static void main(String[] args) {
		SpringApplication.run(OwnlifeApplication.class, args);
	}
}