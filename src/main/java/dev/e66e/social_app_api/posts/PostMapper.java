package dev.e66e.social_app_api.posts;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface PostMapper {

    PostDTO postToPostDTO(Post post);
    Post postDTOToPost(PostDTO postDTO);
}
