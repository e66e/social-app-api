package dev.e66e.social_app_api.posts;

import java.util.UUID;

interface PostRequestHasher {

    String createPostHash(UUID authorId, PostRequest postRequest);
}
