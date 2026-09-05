package dev.e66e.social_app_api.posts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PostDTO(
        @NotBlank UUID id,
        @NotBlank UUID userId,
        String textContent,
        String imageUrl
) {
}
