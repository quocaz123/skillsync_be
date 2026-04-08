package com.skillsync.skillsync.mapper;

import com.skillsync.skillsync.dto.request.auth.AuthenticationRequest;
import com.skillsync.skillsync.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "id", ignore = true)
    User toUser(AuthenticationRequest request);
}

