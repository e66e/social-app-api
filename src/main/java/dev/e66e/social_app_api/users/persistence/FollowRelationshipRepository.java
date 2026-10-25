package dev.e66e.social_app_api.users.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface FollowRelationshipRepository extends JpaRepository<FollowRelationship, FollowRelationshipId> {

    Set<FollowRelationship> findById_followTargetId(UUID followTargetId);

    Set<FollowRelationship> findById_followerId(UUID userId);

    @Query("""
            select r from FollowRelationship r
            where r.id.followerId = :followerId
                and r.id.followTargetId = :followTargetId""")
    Optional<FollowRelationship> findById(@Param("followerId") UUID followerId,
                                          @Param("followTargetId") UUID followTargetId);
}
