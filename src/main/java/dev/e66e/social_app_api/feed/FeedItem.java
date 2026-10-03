package dev.e66e.social_app_api.feed;

import dev.e66e.social_app_api.posts.PostResponse;

public record FeedItem(
        PostResponse post,
        double rankingScore
) {}
