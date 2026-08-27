package dev.e66e.social_app_api.posts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface PostRepository extends JpaRepository<Post, UUID> {

}
