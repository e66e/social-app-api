package dev.e66e.social_app_api.users;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class UsersSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http.securityMatcher("/api/users")
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().permitAll())
                .build();
    }
}
