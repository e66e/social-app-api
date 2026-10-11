package dev.e66e.social_app_api.feed;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "feed")
@Getter @Setter
class FeedProperties {

    private int defaultPageSize;
    private int homeFeedTtlInMinutes;
    private int maxPageSize;
}
