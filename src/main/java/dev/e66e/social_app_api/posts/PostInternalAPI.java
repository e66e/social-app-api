package dev.e66e.social_app_api.posts;

import java.util.Optional;
import java.util.UUID;

public interface PostInternalAPI {

    Optional<PostResponse> getPost(UUID id);
}
