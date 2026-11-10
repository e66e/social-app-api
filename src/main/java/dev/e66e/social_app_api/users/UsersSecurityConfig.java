package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.config.UserOnboardingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class UsersSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain usersSecurityFilterChain(HttpSecurity http, UserOnboardingFilter userOnboardingFilter) {
        return http.securityMatcher("/api/users")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST).authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .addFilterAfter(userOnboardingFilter, BearerTokenAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }
}
