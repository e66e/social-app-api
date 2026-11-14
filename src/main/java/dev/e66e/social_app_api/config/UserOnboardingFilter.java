package dev.e66e.social_app_api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserOnboardingFilter extends OncePerRequestFilter {

    private final OnboardingStatusProvider onboardingAPI;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth) || !auth.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        Jwt jwt = jwtAuth.getToken();

        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            reject(response, HttpStatus.UNAUTHORIZED, "JWT subject is not a UUID.");
            return;
        }

        UUID userId;

        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            reject(response, HttpStatus.UNAUTHORIZED, "JWT subject is not a UUID.");
            return;
        }

        boolean onboarded;
        try {
            onboarded = this.onboardingAPI.isUserOnboarded(userId);
        } catch (RuntimeException e) {
            reject(response, HttpStatus.SERVICE_UNAVAILABLE, "Onboarding status unavailable.");
            return;
        }

        if (!onboarded) {
            log.warn("User {} has not been onboarded yet.", subject);
            reject(response, HttpStatus.FORBIDDEN, "User is not onboarded.");
            return;
        }

        log.info("User {} has been onboarded.", subject);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/onboarding");
    }

    private void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8);

        objectMapper.writeValue(response.getOutputStream(), new ApiError(status, message));
    }
}
