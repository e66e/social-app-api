package dev.e66e.social_app_api.posts;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
interface PostRepository extends JpaRepository<Post, UUID> {

    List<Post> findByAuthorIdInOrderByCreatedAtDescIdDesc(Collection<UUID> authorIds, Pageable pageable);

    @Query("""
                select p from Post p
                where p.authorId in :authorIds
                    and (p.createdAt < :createdAt
                        or (p.createdAt = :createdAt and p.id < :postId))
                            order by p.createdAt desc, p.id desc""")
    List<Post> findHomePageByCreatedAtAndPostId(@Param("authorIds") Collection<UUID> authorIds,
                                                @Param("createdAt") Instant createdAt,
                                                @Param("postId") UUID postId,
                                                Pageable pageable);

    long countByAuthorIdIn(Set<UUID> authorsIds);

    List<Post> findByAuthorIdOrderByCreatedAtDescIdDesc(UUID authorId, Pageable pageable);

    @Query("""
                select p from Post p
                where p.authorId = :authorId
                    and (p.createdAt < :createdAt
                        or (p.createdAt = :createdAt and p.id < :postId))
                            order by p.createdAt desc, p.id desc""")
    List<Post> findByCursor(@Param("authorId") UUID userId,
                           @Param("createdAt") Instant createdAt,
                           @Param("postId") UUID postId,
                           Pageable pageable);

    long countByAuthorId(UUID userId);
}
