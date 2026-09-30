package dev.e66e.social_app_api.posts.idempotency;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "post_creation_request")
class PostCreationRequest implements Persistable<PostCreationRequestId> {

    @EmbeddedId
    private PostCreationRequestId id;

    @Transient
    private boolean isNew = true;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PostCreationStatus status;

    @Column(name = "post_id", unique = true)
    private UUID postId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    @Nullable
    private Instant updatedAt;

    PostCreationRequest(UUID authorId, String idempotencyKey, String requestHash) {
        this.id = new PostCreationRequestId(authorId, idempotencyKey);
        this.requestHash = requestHash;
//        this.isNew = true;
        this.status = PostCreationStatus.IN_PROGRESS;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    void markCompleted(UUID postId) {
        this.postId = postId;
        this.status = PostCreationStatus.COMPLETED;
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    @SuppressWarnings("ConstantValue")
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        //noinspection ConstantValue
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PostCreationRequest that = (PostCreationRequest) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
class PostCreationRequestId implements Serializable {

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;


    @SuppressWarnings("ConstantValue")
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PostCreationRequestId that = (PostCreationRequestId) o;
        return getAuthorId() != null
                && Objects.equals(getAuthorId(), that.getAuthorId())
                && getIdempotencyKey() != null
                && Objects.equals(getIdempotencyKey(), that.getIdempotencyKey());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(authorId, idempotencyKey);
    }
}

enum PostCreationStatus {
    IN_PROGRESS,
    COMPLETED
}
