package dev.e66e.social_app_api.posts;

import jakarta.annotation.Nullable;

import java.util.List;

record PostsPageResponse(
        List<PostResponse> posts,
        @Nullable String nextCursor
) {}
