package dev.e66e.social_app_api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "cors")
@Getter @Setter
class CorsProperties {

    private List<String> allowedOrigins;
}
