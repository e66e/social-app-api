package dev.e66e.social_app_api.comments;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface CommentMapper {

    @Mapping(target = "id", source = "id.id")
    @Mapping(target = "postId", source = "id.postId")
    @Mapping(target = "parentCommentId", source = "parentCommentId")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    CommentResponse commentToCommentResponse(Comment comment);
}
