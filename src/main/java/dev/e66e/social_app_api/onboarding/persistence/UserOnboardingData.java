package dev.e66e.social_app_api.onboarding.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

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
}
