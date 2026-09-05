package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.users.persistence.User;
import dev.e66e.social_app_api.users.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

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
    public UserDTO register(RegistrationData registrationData) {
        User newUser = User.builder()
                .id(registrationData.id())
                .username(registrationData.username())
                .publicUsername(registrationData.publicUsername())
                .avatarUrl(registrationData.avatarUrl())
                .build();
        UserDTO userDTO = mapper.userToUserDTO(this.userRepository.save(newUser));
        this.events.publishEvent(new UserCreatedEvent());
        return userDTO;
    }
}
