package dev.e66e.social_app_api.comments;

import jakarta.annotation.Nullable;

import java.util.List;

record CommentsPageResponse(
        List<CommentResponse> comments,
        @Nullable String nextCursor
) {}
