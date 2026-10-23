package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.users.persistence.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface UserMapper {

    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "isHotUser", source = "hotUser")
    UserRecord userToUserRecord(User user);

}
