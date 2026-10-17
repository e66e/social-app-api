package dev.e66e.social_app_api.idempotency;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractIdempotencyRecord implements Persistable<IdempotencyKey> {

    @EmbeddedId
    private IdempotencyKey id;

    @Transient
    private boolean isNew = true;

    @Column(name = "request_hash", nullable = false, length = 32)
    private String requestHash;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private IdempotencyStatus status;

    @Column(name = "resource_id", unique = true) // nullable?
    private UUID resourceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onPersist() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    protected AbstractIdempotencyRecord(UUID authorId, String idempotencyKey, String requestHash) {
        this.id = new IdempotencyKey(authorId, idempotencyKey);
        this.requestHash = requestHash;
        this.status = IdempotencyStatus.IN_PROGRESS;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    protected void markCompleted(UUID resourceId) {
        this.resourceId = resourceId;
        this.status = IdempotencyStatus.COMPLETED;
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    @Override
    @SuppressWarnings("ConstantValue")
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AbstractIdempotencyRecord that = (AbstractIdempotencyRecord) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}
