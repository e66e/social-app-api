package dev.e66e.social_app_api.feed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_feed_events")
@Getter @Setter
@NoArgsConstructor
class ProcessedFeedEvent {

    @Id
    private UUID id;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    public ProcessedFeedEvent(UUID id) {
        this.id = id;
        this.processedAt = Instant.now();
    }
}
