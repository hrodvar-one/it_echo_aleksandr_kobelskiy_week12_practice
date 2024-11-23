package com.aleksandr_kobelskiy.week12practice.mapper;

import com.aleksandr_kobelskiy.week12practice.dto.UserDto;
import com.aleksandr_kobelskiy.week12practice.entity.UserEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto map(UserEntity userEntity);

    @InheritInverseConfiguration
    UserEntity map(UserDto userDto);
}
