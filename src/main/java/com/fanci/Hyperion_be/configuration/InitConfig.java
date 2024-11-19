package com.fanci.Hyperion_be.configuration;

import com.fanci.Hyperion_be.entity.User;
import com.fanci.Hyperion_be.repository.RoleRepository;
import com.fanci.Hyperion_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class InitConfig {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {
            if (!userRepository.existsUserByUsername("admin")) {
                var role = new HashSet<>(roleRepository.findAll());
                User user = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("123456"))
                        .email("admin")
                        .fullName("admin")
                        .isActive(true)
                        .roles(role)
                        .build();
            userRepository.save(user);
            }else {
                log.warn("already have admin");
            }
        };
    }
}
