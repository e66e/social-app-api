package dev.e66e.social_app_api.users.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FollowRelationshipRepository extends JpaRepository<FollowRelationship, FollowRelationshipId> {

    List<FollowRelationship> findById_followTargetId(UUID followTargetId);
}
