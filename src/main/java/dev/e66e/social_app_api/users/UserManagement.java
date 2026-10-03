package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.users.persistence.User;
import dev.e66e.social_app_api.users.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class UserManagement implements UserExternalAPI, UserInternalAPI, UserRegistration {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final UserMapper mapper;

    @Override
    public boolean isUsernameAvailable(String username) {
        return !this.userRepository.existsByUsername(username);
    }

    @Override
    public UserRecord register(RegistrationData registrationData) {
        User newUser = User.builder()
                .id(registrationData.id())
                .username(registrationData.username())
                .publicUsername(registrationData.publicUsername())
                .avatarUrl(registrationData.avatarUrl())
                .build();
        UserRecord userRecord = mapper.userToUserRecord(this.userRepository.save(newUser));
        this.events.publishEvent(new UserCreatedEvent());
        return userRecord;
    }

    @Override
    public Optional<UserRecord> getUser(UUID userId) {
        return this.userRepository.findById(userId)
                .map(this.mapper::userToUserRecord);
    }
}
