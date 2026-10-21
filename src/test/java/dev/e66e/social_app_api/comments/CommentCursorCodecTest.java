package dev.e66e.social_app_api.comments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentCursorCodecTest {

    @Nested
    class Parse {

        @Test
        void returnsNull_whenCursorIsNull() {
            assertThat(CommentCursorCodec.parse(null)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t\n"})
        void returnsNull_whenCursorIsBlank(String cursor) {
            assertThat(CommentCursorCodec.parse(cursor)).isNull();
        }

        @Test
        void returnsCursor_whenFormatIsValid() {
            // Given
            UUID commentId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();
            long epochMillis = 1_700_000_000_000L;

            // When
            CommentCursor cursor = CommentCursorCodec.parse(commentId + "|" + postId + "|" + epochMillis);

            // Then
            assertThat(cursor).isNotNull();
            assertThat(cursor.commentId()).isEqualTo(commentId);
            assertThat(cursor.postId()).isEqualTo(postId);
            assertThat(cursor.createdAt()).isEqualTo(Instant.ofEpochMilli(epochMillis));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "only-one-part",
                "two|parts",
                "a|b|c|d",
                "not-a-uuid|not-a-uuid|123",
                "|00000000-0000-0000-0000-000000000000|123",
                "00000000-0000-0000-0000-000000000000||123",
                "00000000-0000-0000-0000-000000000000|00000000-0000-0000-0000-000000000000|"
        })
        void throws_whenFormatIsInvalid(String cursor) {
            assertThatThrownBy(() -> CommentCursorCodec.parse(cursor))
                    .isInstanceOf(InvalidParameterException.class);
        }

        @Test
        void throws_whenTimestampIsNotNumber() {
            assertThatThrownBy(() -> CommentCursorCodec.parse(
                    UUID.randomUUID() + "|" + UUID.randomUUID() + "|not-a-number"))
                    .isInstanceOf(InvalidParameterException.class);
        }
    }

    @Nested
    class Encode {
        @Test
        void producesPostIdFirst_thenCommentId_thenEpochMillis() {
            // Given
            UUID commentId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();
            Instant createdAt = Instant.ofEpochMilli(1_700_000_000_000L);
            CommentRecord record = new CommentRecord(commentId, postId, "content", null, createdAt, null);

            // When
            String encoded = CommentCursorCodec.encode(record);

            // Then
            assertThat(encoded).isEqualTo(commentId + "|" + postId + "|" + 1_700_000_000_000L);
        }
    }

    @Test
    void roundTrip_swapsPostIdAndCommentId_dueToFormatMismatch() {
        // Given
        UUID commentId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Instant createdAt = Instant.ofEpochMilli(1_700_000_000_000L);
        CommentRecord record = new CommentRecord(commentId, postId, "content", null, createdAt, null);

        // When
        CommentCursor cursor = CommentCursorCodec.parse(CommentCursorCodec.encode(record));

        // Then
        assertThat(cursor).isNotNull();
        assertThat(cursor.postId()).isEqualTo(postId);
        assertThat(cursor.commentId()).isEqualTo(commentId);
    }

}
