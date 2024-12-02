package com.aleksandr_kobelskiy.week12practice.dto;

import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.entity.UserRole;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserDto {

    private Long id;

    private String username;

    private String password;

    private UserRole role;

    private String firstName;

    private String lastName;

    private Status status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
