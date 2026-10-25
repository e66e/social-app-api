package dev.e66e.social_app_api.users;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
}
