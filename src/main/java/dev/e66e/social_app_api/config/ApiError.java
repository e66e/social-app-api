package dev.e66e.social_app_api.config;

import org.springframework.http.HttpStatus;

record ApiError(
    HttpStatus status,
    String message
) {}
