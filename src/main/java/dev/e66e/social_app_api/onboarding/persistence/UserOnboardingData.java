package dev.e66e.social_app_api.onboarding.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_onboarding")
public class UserOnboardingData {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false)
    @ColumnDefault("'PENDING'")
    private UserOnboardingStatus onboardingStatus;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        UserOnboardingData that = (UserOnboardingData) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }
}
