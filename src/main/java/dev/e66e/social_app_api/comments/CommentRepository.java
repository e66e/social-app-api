package dev.e66e.social_app_api.comments;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface CommentRepository extends JpaRepository<Comment, CommentId> {

    @Query("""
            select c from Comment c
            where c.id.id = :commentId
                and c.id.postId = :postId""")
    Optional<Comment> findById(@Param("commentId") UUID commentId,
                              @Param("postId") UUID postId);

    @Query("""
            select c from Comment c
            where c.id.postId = :postId
            order by c.createdAt desc""")
    List<Comment> findAllPostCommentByNewest(@Param("postId") UUID postId, Pageable pageable);

    @Query("""
            select c from Comment c
            where c.id.postId = :postId and
                (c.createdAt < :createdAt or (
                    c.createdAt = :createdAt and c.id.id < :commentId))
            order by c.createdAt, c.id.id""")
    List<Comment> findAllPostCommentAfterCursor(@Param("postId") UUID postId,
                                                @Param("commentId") UUID commentId,
                                                @Param("createdAt") Instant createdAt,
                                                PageRequest pageable);

    @Query("""
            select c from Comment c
            where c.authorId = :userId
            order by c.createdAt desc""")
    List<Comment> findAllUserCommentByNewest(@Param("userId") UUID userId, PageRequest pageable);

    @Query("""
            select c from Comment c
            where c.authorId = :authorId and
                (c.createdAt < :createdAt or (
                    c.createdAt = :createdAt and c.id.id < :commentId))
            order by c.createdAt, c.id.id""")
    List<Comment> findAllUserCommentAfterCursor(@Param("authorId") UUID authorId,
                                                @Param("updateId") UUID commentId,
                                                @Param("createdAt") Instant createdAt,
                                                PageRequest pageable);
}
