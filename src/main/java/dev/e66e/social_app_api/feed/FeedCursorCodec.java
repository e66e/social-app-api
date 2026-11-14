package dev.e66e.social_app_api.feed;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
class FeedCursorCodec {

    private static final String SEPARATOR = "|";
    private static final String SEPARATOR_REGEX = Pattern.quote(SEPARATOR);

    @Nullable
    static FeedCursor parse(@Nullable String cursorString) {

        if (cursorString == null || cursorString.isBlank()) {
            return null;
        }

        log.debug("Parsing FeedCursor String: {}", cursorString);
        String[] parts = cursorString.split(SEPARATOR_REGEX, 2);

        if (parts.length != 2 || parts[1].isBlank()) {
            log.debug("Invalid FeedCursor String: {}", cursorString);
            throw new InvalidParameterException("Invalid cursor string: " + cursorString);
        }

        try {
            long epochMillis = Long.parseLong(parts[0]);
            return new FeedCursor(Instant.ofEpochMilli(epochMillis), UUID.fromString(parts[1]));
        } catch (RuntimeException e) {
            log.debug("Invalid FeedCursor timestamp: {}", cursorString);
            throw new InvalidParameterException("Invalid cursor string: " + cursorString);
        }
    }

    static String encode(FeedItem item) {
        return encode(item.post().createdAt(), item.post().postId());
    }

    private static String encode(Instant createdAt, UUID postId) {
        return createdAt.toEpochMilli() + SEPARATOR + postId;
    }
}
