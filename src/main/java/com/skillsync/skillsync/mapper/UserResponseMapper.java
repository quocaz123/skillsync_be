package com.skillsync.skillsync.mapper;

import com.skillsync.skillsync.dto.response.UserResponse;
import com.skillsync.skillsync.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserResponseMapper {
    UserResponse toResponse(User user);
}
