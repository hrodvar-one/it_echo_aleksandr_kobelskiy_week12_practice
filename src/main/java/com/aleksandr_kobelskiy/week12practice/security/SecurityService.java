package com.aleksandr_kobelskiy.week12practice.security;

import com.aleksandr_kobelskiy.week12practice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public Mono<TokenDetails> authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .flatMap(user -> {
//                    if(!user.isEnabled()) {
                    if(!user.getStatus()) {
                        return Mono.error(new RuntimeException("User is not enabled"));
                    }

                    if(!passwordEncoder.matches(password, user.getPassword())) {
                        return Mono.error(new RuntimeException(""));
                    }

                    return Mono.just(new TokenDetails());
                })
                .switchIfEmpty(Mono.error(new RuntimeException("")));
    }
}
