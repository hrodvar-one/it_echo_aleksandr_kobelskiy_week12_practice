package com.aleksandr_kobelskiy.week12practice.dto;

import com.aleksandr_kobelskiy.week12practice.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestDto {

    private String firstName;

    private String lastName;

    private UserRole role;
}
