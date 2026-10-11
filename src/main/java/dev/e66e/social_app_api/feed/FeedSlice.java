package dev.e66e.social_app_api.feed;

import java.util.List;

record FeedSlice(
       List<FeedItem> items,
       boolean hasMore
) {}
