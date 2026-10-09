package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.users.persistence.FollowRelationship;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface FollowRelationshipMapper {

    FollowRelationshipRecord followRelationshipToRecord(FollowRelationship followRelationship);

    FollowRelationship recordToFollowRelationship(FollowRelationshipRecord followRelationship);
}
