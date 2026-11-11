package dev.e66e.social_app_api.comments;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
class CommentCursorCodec {

    private static final String SEPARATOR = "|";
    private static final String SEPARATOR_REGEX = Pattern.quote(SEPARATOR);

    // cursor = commentId|postId|createdAt in epoch millis
    @Nullable
    static CommentCursor parse(@Nullable String cursorString) {
        log.debug("Parsing comments cursor string: {}", cursorString);
        if (cursorString == null || cursorString.isBlank()) {
            log.debug("Invalid cursor string: {}", cursorString);
            return null;
        }

        String[] parts = cursorString.split(SEPARATOR_REGEX, 3);

        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            log.debug("Invalid cursor string: {}", cursorString);
            throw new InvalidParameterException("Invalid cursor string: " + cursorString);
        }

        try {
            long epochMillis = Long.parseLong(parts[2]);
            log.debug("Creating CommentCursor with: commentId: {}, postId: {}, timestamp: {}",
                    parts[0], parts[1], epochMillis);
            return new CommentCursor(UUID.fromString(parts[0]),
                    UUID.fromString(parts[1]),
                    Instant.ofEpochMilli(epochMillis));
        } catch (RuntimeException e) {
            log.debug("Problem with parsing timestamp for input: {}", cursorString);
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
