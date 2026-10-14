package dev.e66e.social_app_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableCaching
@ConfigurationPropertiesScan
@EnableScheduling
public class SocialAppAPIApplication {

	static void main(String[] args) {
		SpringApplication.run(SocialAppAPIApplication.class, args);
	}
}
