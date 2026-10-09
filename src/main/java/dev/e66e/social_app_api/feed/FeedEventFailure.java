package dev.e66e.social_app_api.feed;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feed_event_failures")
@Getter @Setter
@NoArgsConstructor
class FeedEventFailure {

    @Id
    private UUID id;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", nullable = false, length = 1024)
    private String lastError;

    @Column(name = "first_failed_at", nullable = false, updatable = false)
    private Instant firstFailedAt;

    @Column(name = "last_failed_at", nullable = false)
    private Instant lastFailedAt;

    public FeedEventFailure(UUID id) {
        this.id = id;
        this.attemptCount = 0;
        this.lastError = "UNSET";
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.firstFailedAt = now;
        this.lastFailedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.lastFailedAt = Instant.now();
    }

    public int recordFailure(String error) {
        this.lastError = error;
        this.lastFailedAt = Instant.now();
        return ++this.attemptCount;
    }
}
