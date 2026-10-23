package dev.e66e.social_app_api.comments;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
class CommentController {

    private final CommentExternalAPI commentAPI;

    @GetMapping("/{postId}/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentResponse getComment(@PathVariable("postId") UUID postId,
                                      @PathVariable("commentId") UUID commentId) {
        return this.commentAPI.getComment(commentId, postId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found."));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CommentsPageResponse getPostComments(@PathVariable("id") UUID postId,
                                                @RequestParam(name = "cursor", required = false)
                                                String cursor,
                                                @RequestParam(name = "limit", defaultValue = "${comments.default-page-size}") @Min(1)
                                                int limit) {
        return this.commentAPI.getPostComments(postId, cursor, limit);
    }

    @GetMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentsPageResponse getUserComments(@PathVariable("userId") UUID userId,
                                                @RequestParam(name = "cursor", required = false)
                                                String cursor,
                                                @RequestParam(defaultValue = "${comments.default-page-size}")
                                                int limit) {
        return this.commentAPI.getUserComments(userId, cursor, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse postComment(@RequestBody @Valid CreateCommentRequest commentRequest,
                                       @AuthenticationPrincipal Jwt jwt) {
        UUID authorId = UUID.fromString(Objects.requireNonNull(jwt.getSubject()));

        return this.commentAPI.postComment(authorId,
                commentRequest.idempotencyKey(),
                commentRequest.commentRequest());
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<String> handleConstraintViolationException(Exception e) {
        return ResponseEntity
                .badRequest()
                .body(e.getMessage());
    }
}
