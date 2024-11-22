package com.aleksandr_kobelskiy.week12practice.rest;

import com.aleksandr_kobelskiy.week12practice.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserRestControllerV1 {

    @PostMapping
    public User create(User user) {
        return new User();
    }
}
