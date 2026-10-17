package dev.e66e.social_app_api.idempotency;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.validator.constraints.Length;

import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey {

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "idempotency_key")
    @Length(min = 32, max = 64)
    private String idempotencyKey;


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
        IdempotencyKey that = (IdempotencyKey) o;
        return getAuthorId() != null && Objects.equals(getAuthorId(), that.getAuthorId())
                && getIdempotencyKey() != null && Objects.equals(getIdempotencyKey(), that.getIdempotencyKey());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(authorId, idempotencyKey);
    }
}
