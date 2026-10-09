package dev.e66e.social_app_api.feed;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letter_feed_events")
@Getter @Setter
@NoArgsConstructor
class DeadLetterFeedEvent {

    @Id
    private UUID id;

    private UUID eventId;

    private String streamRecordId;

    private String payloadJson;

    private String failureReason;

    private int attempts;

    private Instant movedToDlqAt;

    public DeadLetterFeedEvent(int attempts,
                               String failureReason,
                               String payloadJson,
                               String streamRecordId,
                               UUID eventId) {
        this.attempts = attempts;
        this.failureReason = failureReason;
        this.payloadJson = payloadJson;
        this.streamRecordId = streamRecordId;
        this.eventId = eventId;
    }

    @PrePersist
    void onCreate() {
        this.movedToDlqAt = Instant.now();
    }
}
