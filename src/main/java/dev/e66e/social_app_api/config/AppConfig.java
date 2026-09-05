package dev.e66e.social_app_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AppConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OnboardedAuthorizationManager onboardingManager) {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**").access(onboardingManager));

        return http.build();
    }
}
