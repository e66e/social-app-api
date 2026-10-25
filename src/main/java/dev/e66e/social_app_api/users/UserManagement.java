package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.users.idempotency.FollowRelationshipIdempotencyAPI;
import dev.e66e.social_app_api.users.persistence.FollowRelationship;
import dev.e66e.social_app_api.users.persistence.FollowRelationshipRepository;
import dev.e66e.social_app_api.users.persistence.User;
import dev.e66e.social_app_api.users.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class UserManagement implements UserExternalAPI, UserInternalAPI, UserRegistration {

    private final UserRepository userRepository;
    private final FollowRelationshipRepository followRepository;
    private final UserMapper userMapper;
    private final FollowRelationshipMapper followMapper;
    private final FollowRelationshipHasher followHasher;
    private final FollowRelationshipIdempotencyAPI followIdempotencyAPI;
    private final ApplicationEventPublisher events;

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
        UserRecord userRecord = userMapper.userToUserRecord(this.userRepository.saveAndFlush(newUser));
        this.events.publishEvent(new UserCreatedEvent());
        return userRecord;
    }

    @Override
    public Optional<UserRecord> getUser(UUID userId) {
        return this.userRepository.findById(userId)
                .map(this.userMapper::userToUserRecord);
    }

    @Override
    public Set<UUID> getUsersFollowersIds(UUID userId) {
        Set<FollowRelationship> follows = this.followRepository.findById_followTargetId(userId);

        return follows.stream()
                .map(FollowRelationship::getFollowerId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<UUID> getFollowTargetIds(UUID follower) {
        Set<FollowRelationship> userFollowTargets = this.followRepository.findById_followerId(follower);

        return userFollowTargets.stream()
                .map(FollowRelationship::getFollowTargetId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<UserRecord> getUserGroup(Set<UUID> userIds) {
        Set<User> users = this.userRepository.findByIdIn(userIds);

        return users.stream()
                .map(this.userMapper::userToUserRecord)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<UserResponse> getUserById(UUID userId) {
        return this.getUser(userId).map(this.userMapper::userRecordToUserResponse);
    }


    @Override
    public Optional<UserResponse> getUserByPublicUsername(String username) {
        return this.getUserByPubUsername(username).map(this.userMapper::userRecordToUserResponse);
    }

    @Override
    @Transactional
    public FollowRelationshipResponse followUser(UUID followerId, UUID followTargetId, String idempotencyKey) {
        String relationHash = this.followHasher.createRelationshipHash(followerId, followTargetId);

        User follower = this.userRepository.findById(followerId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Follower not found."));

        User followTarget = this.userRepository.findById(followTargetId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Follow target not found."));

        UUID newRecordsFollowTargetId = this.followIdempotencyAPI.executeIdempotent(followerId, idempotencyKey, relationHash, () -> {
            FollowRelationship followRelationship = new FollowRelationship(follower, followTarget);

            return this.followRepository.saveAndFlush(followRelationship).getFollowTargetId();
        });

        FollowRelationship newRelationship = this.followRepository.findById(followerId, newRecordsFollowTargetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Record was added but wasn't found in database."));

        return this.followMapper.followRelationshipToFollowRelationshipResponse(newRelationship);
    }

    private Optional<UserRecord> getUserByPubUsername(String username) {
        return this.userRepository.findByPublicUsername(username).map(this.userMapper::userToUserRecord);
    }
}
