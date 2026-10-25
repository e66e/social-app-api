package dev.e66e.social_app_api.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class AppConfig {

    @Bean
    public FilterRegistrationBean<UserOnboardingFilter> disableOnboardingFilterAutoRegistration(
            UserOnboardingFilter filter) {
        FilterRegistrationBean<UserOnboardingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
