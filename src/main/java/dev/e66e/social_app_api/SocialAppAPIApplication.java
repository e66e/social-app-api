package dev.e66e.social_app_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SocialAppAPIApplication {

	static void main(String[] args) {
		SpringApplication.run(SocialAppAPIApplication.class, args);
	}
}
