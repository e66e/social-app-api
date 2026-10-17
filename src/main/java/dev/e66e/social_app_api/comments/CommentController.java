package dev.e66e.social_app_api.comments;

import dev.e66e.social_app_api.posts.PostResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
class CommentController {

    private final CommentExternalAPI commentAPI;

    @GetMapping("/{id}")
    public List<CommentResponse> getPostComments(@PathVariable("id") UUID postId) {
        return this.commentAPI.getPostComments(postId);
    }

    public List<CommentResponse> getUserComments(@PathVariable("id") UUID userId) {
        return this.commentAPI.getUserComments(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public PostResponse postComment(@RequestBody @Valid CommentRequest commentRequest) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
