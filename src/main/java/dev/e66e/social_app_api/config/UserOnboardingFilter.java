package dev.e66e.social_app_api.config;

import dev.e66e.social_app_api.onboarding.OnboardingInternalAPI;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class UserOnboardingFilter extends OncePerRequestFilter {

    private final OnboardingInternalAPI onboardingAPI;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String keycloakUserId = jwtAuth.getToken().getClaimAsString("sub");

            if (keycloakUserId == null
                    || this.onboardingAPI.isUserOnboarded(UUID.fromString(keycloakUserId))) {

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("""
                        {
                            "error": User onboarding incomplete. Please submit required information."
                        }""");
            }
        }

        filterChain.doFilter(request, response);
    }
}
