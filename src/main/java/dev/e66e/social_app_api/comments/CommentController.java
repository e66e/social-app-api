package dev.e66e.social_app_api.comments;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comment")
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
    public List<CommentResponse> getPostComments(@PathVariable("id") UUID postId,
                                                 @RequestParam(name = "cursor", required = false)
                                                 String cursor,
                                                 @RequestParam(name = "limit", defaultValue = "10")
                                                 int limit) {
        return this.commentAPI.getPostComments(postId, cursor, limit);
    }

    public List<CommentResponse> getUserComments(@PathVariable("id") UUID userId) {
//        return this.commentAPI.getUserComments(userId);
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public CommentResponse postComment(@RequestBody @Valid CommentRequest commentRequest) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
