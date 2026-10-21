package dev.e66e.social_app_api.comments;

import jakarta.annotation.Nullable;

import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

class CommentCursorCodec {

    private static final String SEPARATOR = "|";
    private static final String SEPARATOR_REGEX = Pattern.quote(SEPARATOR);

    // cursor = commentId|postId|createdAt in epoch millis
    @Nullable
    static CommentCursor parse(@Nullable String cursorString) {
        if (cursorString == null || cursorString.isBlank()) {
            return null;
        }

        String[] parts = cursorString.split(SEPARATOR_REGEX, 3);

        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new InvalidParameterException("Invalid cursor string: " + cursorString);
        }

        try {
            long epochMillis = Long.parseLong(parts[2]);
            return new CommentCursor(UUID.fromString(parts[0]),
                    UUID.fromString(parts[1]),
                    Instant.ofEpochMilli(epochMillis));
        } catch (RuntimeException e) {
            throw new InvalidParameterException("Invalid cursor string: " + cursorString);
        }
    }

    static String encode(CommentRecord commentRecord) {
        return encode(commentRecord.id(), commentRecord.postId(), commentRecord.createdAt());
    }

    private static String encode(UUID commentId, UUID postId, Instant createdAt) {
        return commentId
                + SEPARATOR + postId
                + SEPARATOR + createdAt.toEpochMilli();
    }
}
