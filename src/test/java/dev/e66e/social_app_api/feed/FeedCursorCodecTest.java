package dev.e66e.social_app_api.feed;

import dev.e66e.social_app_api.posts.PostResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedCursorCodecTest {

    @Nested
    class ParseMethodTests {

        @Test
        void nullOrBlankInput_returnNull() {
            assertThat(FeedCursorCodec.parse(null)).isNull();
            assertThat(FeedCursorCodec.parse("")).isNull();
            assertThat(FeedCursorCodec.parse("   ")).isNull();
        }

        @Test
        void validCursor_returnsFeedCursor() {
            UUID postId = UUID.randomUUID();
            Instant createdAt = Instant.ofEpochMilli(1_700_000_000_000L);
            String cursor = createdAt.toEpochMilli() + "|" + postId;

            FeedCursor parsed = FeedCursorCodec.parse(cursor);

            assertThat(parsed).isNotNull();
            assertThat(parsed.createdAt()).isEqualTo(createdAt);
            assertThat(parsed.postId()).isEqualTo(postId);
        }

        @Test
        void invalidCursor_throws() {
            assertThatThrownBy(() -> FeedCursorCodec.parse("not-a-cursor"))
                    .isInstanceOf(InvalidParameterException.class);

            assertThatThrownBy(() -> FeedCursorCodec.parse("123|not-a-uuid"))
                    .isInstanceOf(InvalidParameterException.class);
        }
    }

    @Test
    @DisplayName("Encode method test. Encoding correctly created FeedItem.")
    void encode_item_returnsEpochMillisAndPostId() {
        UUID postId = UUID.randomUUID();
        Instant createdAt = Instant.ofEpochMilli(1_700_000_000_000L);

        PostResponse post = new PostResponse(
                postId,
                UUID.randomUUID(),
                "alice",
                createdAt,
                null,
                "hello",
                null
        );

        FeedItem item = new FeedItem(post, 1.0);

        String encoded = FeedCursorCodec.encode(item);

        assertThat(encoded).isEqualTo(createdAt.toEpochMilli() + "|" + postId);
    }
}