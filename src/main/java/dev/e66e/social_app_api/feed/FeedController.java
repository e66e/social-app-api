package dev.e66e.social_app_api.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
class FeedController {

    private final FeedExternalAPI feedAPI;

    @GetMapping("/home")
    @ResponseStatus(HttpStatus.OK)
    public TimelinePage getHomeFeed(@RequestParam(required = false) String cursor,
                                    @RequestParam(defaultValue = "5") int limit,
                                    @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));

        return this.feedAPI.getHomeFeed(userId, cursor, limit);
    }

    @GetMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public TimelinePage getUserFeed(@PathVariable("userId") UUID userId,
                                    @RequestParam(required = false) String cursor,
                                    @RequestParam(defaultValue = "5") int limit) {
        return this.feedAPI.getUserFeed(userId, cursor, limit);
    }
}
