package dev.e66e.social_app_api.posts;

import jakarta.annotation.Nullable;

import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

class PostCursorCodec {

    private static final String SEPARATOR = "|";
    private static final String SEPARATOR_REGEX = Pattern.quote(SEPARATOR);

    // cursor = postId|createdAt in epoch millis
    @Nullable
    static PostCursor parse(@Nullable String cursorString) {
        if (cursorString == null || cursorString.isBlank()) {
            return null;
        }

        String[] parts = cursorString.split(SEPARATOR_REGEX, 2);

        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new InvalidParameterException("Invalid cursor string: " + cursorString);
        }

        try {
            long epochMillis = Long.parseLong(parts[1]);
            return new PostCursor(UUID.fromString(parts[0]),
                    Instant.ofEpochMilli(epochMillis));
        } catch (RuntimeException e) {
            throw new InvalidParameterException("Invalid cursor string: " + cursorString);
        }
    }

    static String encode(PostRecord postRecord) {
        return encode(postRecord.id(), postRecord.createdAt());
    }

    private static String encode(UUID postId, Instant createdAt) {
        return postId
                + SEPARATOR + createdAt.toEpochMilli();
    }

}
