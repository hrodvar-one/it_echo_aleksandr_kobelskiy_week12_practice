package com.aleksandr_kobelskiy.week12practice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Table ("users")
public class UserEntity {

    @Id
    private Long id;

    private String username;

    private String password;

    private UserRole role;

    private String firstName;

    private String lastName;

    private Status status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

//    @Transient
//    private List<Event> events;

    @ToString.Include(name = "password")
    private String maskPassword() {
        return "********";
    }
}
