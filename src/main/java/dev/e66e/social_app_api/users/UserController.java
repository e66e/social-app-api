package dev.e66e.social_app_api.users;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
class UserController {

    private final UserExternalAPI userAPI;

    @GetMapping("/{username}")
    public UserResponse getUser(@PathVariable String username) {
        log.info("Getting user with username={}.", username);
        return this.userAPI.getUserByUsername(username)
            .orElseThrow(() -> {
                log.warn("User with username={} not found.", username);
                return new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User with given identifier was not found.");
            });
    }

    @PostMapping
    public FollowRelationshipResponse followUser(
            @RequestBody @Valid
            FollowRelationshipRequest followRelationshipRequest,
            @AuthenticationPrincipal Jwt jwt) {
        UUID followerId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));
        log.info("User with id={} is trying to follow user with username={}.",
                followerId, followRelationshipRequest.followTargetUsername());

        return this.userAPI.followUser(followerId,
                followRelationshipRequest.followTargetUsername(),
                followRelationshipRequest.idempotencyKey());
    }
}
