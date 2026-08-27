package dev.e66e.social_app_api.posts;

import java.util.UUID;

public record PostDTO(
        UUID id,
        UUID userId,
        String textContent,
        String imageUrl
) {
}
