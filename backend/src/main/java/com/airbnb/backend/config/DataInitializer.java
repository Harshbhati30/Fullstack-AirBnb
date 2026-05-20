package com.airbnb.backend.config;

import com.airbnb.backend.entity.Role;
import com.airbnb.backend.entity.User;
import com.airbnb.backend.enums.RoleName;
import com.airbnb.backend.repository.RoleRepository;
import com.airbnb.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
                log.info("Created role: {}", roleName);
            }
        }
    }

    private void seedAdminUser() {
        if (userRepository.findByEmail("admin@airbnb.com").isEmpty()) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow();
            Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                    .orElseThrow();

            User admin = User.builder()
                    .firstName("Platform")
                    .lastName("Admin")
                    .email("admin@airbnb.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .isActive(true)
                    .isEmailVerified(true)
                    .provider("LOCAL")
                    .roles(Set.of(adminRole, userRole))
                    .build();

            userRepository.save(admin);
            log.info("Admin user created — email: admin@airbnb.com password: Admin@123");
        }
    }
}