package dev.e66e.social_app_api.comments;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SHA256CommentRequestHasherTest {

    private final SHA256CommentRequestHasher hasher = new SHA256CommentRequestHasher();

    @Test
    void produces64CharacterHex() {
        // Given / When
        String hash = hasher.createCommentHash(
                UUID.randomUUID(),
                new CommentRequest(UUID.randomUUID(), null, "hello"));

        // Then
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void producesStableHashForSameInput() {
        // Given
        UUID authorId = UUID.randomUUID();
        CommentRequest request = new CommentRequest(UUID.randomUUID(), null, "hello");

        // When / Then
        assertThat(hasher.createCommentHash(authorId, request))
                .isEqualTo(hasher.createCommentHash(authorId, request));
    }

    @Test
    void producesDifferentHashForDifferentAuthor() {
        // Given
        CommentRequest request = new CommentRequest(UUID.randomUUID(), null, "hello");

        // When / Then
        assertThat(hasher.createCommentHash(UUID.randomUUID(), request))
                .isNotEqualTo(hasher.createCommentHash(UUID.randomUUID(), request));
    }

    @Test
    void producesDifferentHashForDifferentPost() {
        // Given
        UUID authorId = UUID.randomUUID();

        // When / Then
        assertThat(hasher.createCommentHash(authorId,
                new CommentRequest(UUID.randomUUID(), null, "hello")))
                .isNotEqualTo(hasher.createCommentHash(authorId,
                        new CommentRequest(UUID.randomUUID(), null, "hello")));
    }

    @Test
    void producesDifferentHashForDifferentContent() {
        // Given
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();

        // When / Then
        assertThat(hasher.createCommentHash(authorId, new CommentRequest(postId, null, "a")))
                .isNotEqualTo(hasher.createCommentHash(authorId, new CommentRequest(postId, null, "b")));
    }

    @Test
    void producesDifferentHashForDifferentParentCommentId() {
        // Given
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        String content = "hello";

        // When
        String hash1 = hasher.createCommentHash(authorId,
                new CommentRequest(postId, UUID.randomUUID(), content));
        String hash2 = hasher.createCommentHash(authorId,
                new CommentRequest(postId, UUID.randomUUID(), content));

        // Then
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
