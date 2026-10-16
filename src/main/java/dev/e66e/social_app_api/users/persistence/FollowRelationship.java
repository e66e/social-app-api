package dev.e66e.social_app_api.users.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class FollowRelationship {

    @EmbeddedId
    private FollowRelationshipId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followerId")
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followTargetId")
    @JoinColumn(name = "follow_target_id", nullable = false)
    private User followTarget;

    public FollowRelationship(User follower, User followTarget) {
        this.id = new FollowRelationshipId(follower.getId(), followTarget.getId());
        this.follower = follower;
        this.followTarget = followTarget;
    }

    public UUID getFollowerId() {
        return this.id.getFollowerId();
    }

    public UUID getFollowTargetId() {
        return this.id.getFollowTargetId();
    }

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
        FollowRelationship that = (FollowRelationship) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}

@Embeddable
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
class FollowRelationshipId {

    @Column(name = "follower_id", nullable = false)
    private UUID followerId;

    @Column(name = "follow_target_id", nullable = false)
    private UUID followTargetId;
}
