package dev.e66e.social_app_api.feed;

import dev.e66e.social_app_api.posts.PostResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FeedItemTest {

    @Test
    @DisplayName("FeedItem objects compare test.")
    void compareTo_ordersByRankingScore() {
        FeedItem low = item(1.0);
        FeedItem high = item(5.0);

        assertThat(low.compareTo(high)).isNegative();
        assertThat(high.compareTo(low)).isPositive();
        assertThat(low.compareTo(item(1.0))).isZero();
    }

    private FeedItem item(double score) {
        UUID postId = UUID.randomUUID();
        Instant now = Instant.now();

        return new FeedItem(
                new PostResponse(
                        postId,
                        UUID.randomUUID(),
                        "user",
                        now,
                        now,
                        "text",
                        null
                ),
                score
        );
    }
}