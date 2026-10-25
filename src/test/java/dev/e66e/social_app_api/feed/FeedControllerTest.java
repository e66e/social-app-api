package dev.e66e.social_app_api.feed;

import dev.e66e.social_app_api.onboarding.OnboardingInternalAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(FeedController.class)
@TestPropertySource(properties = {
        "feed.default-page-size=5",
        "feed.max-page-size=20"
})
@AutoConfigureRestTestClient
class FeedControllerTest {

    @MockitoBean
    private FeedExternalAPI feedAPI;

    @MockitoBean
    private OnboardingInternalAPI onboardingAPI;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private RestTestClient restClient;

    @BeforeEach
    void setUp() {
        lenient().when(onboardingAPI.isUserOnboarded(any())).thenReturn(true);
    }

    @Nested
    class GetHomeFeed {

        @Test
        @WithMockUser
        void shouldReturnHomeFeedForAuthenticatedUser() {
            UUID userId = UUID.randomUUID();
            TimelinePage page = new TimelinePage(userId, 0, List.of(), null);

            when(feedAPI.getHomeFeed(eq(userId), eq("cur1"), eq(5))).thenReturn(page);

            Jwt mockJwt = Jwt.withTokenValue("feed-token")
                    .header("alg", "none")
                    .subject(userId.toString())
                    .build();

            when(jwtDecoder.decode("feed-token")).thenReturn(mockJwt);

            restClient.get()
                    .uri(builder -> builder
                            .path("/api/feed/home")
                            .queryParam("cursor", "cur1")
                            .queryParam("limit", 5)
                            .build())
                    .header("Authorization", "Bearer feed-token")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.ownerId").isEqualTo(userId.toString());

            verify(feedAPI).getHomeFeed(eq(userId), eq("cur1"), eq(5));
        }
    }

    @Nested
    class GetUserFeed {

        @Test
        @WithMockUser
        void shouldReturnUserFeed() {
            UUID targetUserId = UUID.randomUUID();
            TimelinePage page = new TimelinePage(targetUserId, 0, List.of(), null);

            when(feedAPI.getUserFeed(eq(targetUserId), eq("cursor-abc"), eq(10))).thenReturn(page);

            restClient.get()
                    .uri(builder -> builder
                            .path("/api/feed/user/{userId}")
                            .queryParam("cursor", "cursor-abc")
                            .queryParam("limit", 10)
                            .build(targetUserId))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.ownerId").isEqualTo(targetUserId.toString());

            verify(feedAPI).getUserFeed(eq(targetUserId), eq("cursor-abc"), eq(10));
        }
    }
}
