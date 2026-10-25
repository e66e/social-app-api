package dev.e66e.social_app_api.users;

import dev.e66e.social_app_api.users.persistence.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
interface UserMapper {

    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "isHotUser", source = "hotUser")
    UserRecord userToUserRecord(User user);

    UserResponse userRecordToUserResponse(UserRecord userRecord);
}
