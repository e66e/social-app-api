package dev.e66e.social_app_api.feed;

import dev.e66e.social_app_api.posts.PostPublishedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class FeedManagement implements FeedExternalAPI, FeedInternalAPI {

    private final StringRedisTemplate redis;


    public void onPostPublished(PostPublishedEvent event) {

    }
}
