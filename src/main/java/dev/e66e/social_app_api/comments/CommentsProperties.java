package dev.e66e.social_app_api.comments;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "comments")
@Getter @Setter
class CommentsProperties {

    private int defaultPageSize;
    private int maxPageSize;
}
