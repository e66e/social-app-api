package dev.e66e.social_app_api.posts;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

public record PostRequest(
        @NotBlank String content,
        @Nullable String imageUrl
) {
    public PostRequest {
        content = content.trim();

        if (imageUrl != null)
            imageUrl = imageUrl.trim();
        else
            imageUrl = "";
    }
}
