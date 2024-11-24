package com.aleksandr_kobelskiy.week12practice.security;

import com.aleksandr_kobelskiy.week12practice.entity.Status;
import com.aleksandr_kobelskiy.week12practice.exception.UnauthorizedException;
import com.aleksandr_kobelskiy.week12practice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager {

    private final UserService userService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        return userService.getUserById(principal.getId())
                .filter(user -> user.getStatus() == Status.ACTIVE)
                .switchIfEmpty(Mono.error(new UnauthorizedException("User not active")))
                .map(user -> authentication);
    }
}
