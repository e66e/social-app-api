package dev.e66e.social_app_api.feed;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_feed_events")
@Getter @Setter
@NoArgsConstructor
class ProcessedFeedEvent implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    @Transient
    private boolean isNew = true;

    public ProcessedFeedEvent(UUID id) {
        this.id = id;
        this.processedAt = Instant.now();
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PrePersist
    void makeNowNew() {
        this.isNew = false;
    }
}
