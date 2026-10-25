package dev.e66e.social_app_api.users;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
class UserController {

    private final UserExternalAPI userAPI;

    @GetMapping("/{identifier}")
    public UserResponse getUser(@PathVariable String identifier) {
        try {
            UUID userId = UUID.fromString(identifier);
            return this.userAPI.getUserById(userId)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "User with given identifier was not found."));
        } catch (IllegalArgumentException e) {
            return this.userAPI.getUserByPublicUsername(identifier)
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "User with given identifier was not found."));
        }
    }

    @PostMapping
    public FollowRelationshipResponse followUser(
            @RequestBody
            @Valid FollowRelationshipRequest followRelationshipRequest,
            @AuthenticationPrincipal Jwt jwt) {
        UUID followerId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));

        return this.userAPI.followUser(followerId,
                followRelationshipRequest.followTargetId(),
                followRelationshipRequest.idempotencyKey());
    }
}
