package dev.e66e.social_app_api.posts;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "posts")
@Getter @Setter
class PostsProperties {

    private int defaultPageSize;
    private int maxPageSize;
}
