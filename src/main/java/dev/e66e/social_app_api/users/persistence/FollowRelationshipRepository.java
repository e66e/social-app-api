package dev.e66e.social_app_api.users.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface FollowRelationshipRepository extends JpaRepository<FollowRelationship, FollowRelationshipId> {

    Set<FollowRelationship> findById_followTargetId(UUID followTargetId);

    Set<FollowRelationship> findById_followerId(UUID userId);
}
