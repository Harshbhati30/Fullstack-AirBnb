package com.airbnb.backend.config;

import com.airbnb.backend.entity.Amenity;
import com.airbnb.backend.entity.Role;
import com.airbnb.backend.entity.User;
import com.airbnb.backend.enums.RoleName;
import com.airbnb.backend.repository.AmenityRepository;
import com.airbnb.backend.repository.RoleRepository;
import com.airbnb.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final AmenityRepository amenityRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@airbnb.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        seedRoles();
        seedAmenities();
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

    private void seedAmenities() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("WiFi", "wifi");
        defaults.put("Air Conditioning", "snowflake");
        defaults.put("Kitchen", "utensils");
        defaults.put("Free Parking", "parking");
        defaults.put("Swimming Pool", "swimming-pool");
        defaults.put("TV", "tv");
        defaults.put("Washing Machine", "washer");
        defaults.put("Heating", "fire");
        defaults.put("Hot Tub", "hot-tub");
        defaults.put("Gym", "dumbbell");
        defaults.put("Balcony", "balcony");
        defaults.put("Beach Access", "umbrella-beach");
        defaults.put("Pet Friendly", "paw");
        defaults.put("Dedicated Workspace", "laptop");
        defaults.put("Breakfast Included", "coffee");
        defaults.put("Power Backup", "bolt");

        defaults.forEach((name, icon) -> {
            if (amenityRepository.findByNameIgnoreCase(name).isEmpty()) {
                amenityRepository.save(Amenity.builder().name(name).icon(icon).build());
                log.info("Created amenity: {}", name);
            }
        });
    }

    private void seedAdminUser() {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
            Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow();

            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            roles.add(userRole);

            User admin = User.builder()
                    .firstName("Platform")
                    .lastName("Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .isActive(true)
                    .isEmailVerified(true)
                    .provider("LOCAL")
                    .roles(roles)
                    .build();

            userRepository.save(admin);
            log.info("Admin user created: {}", adminEmail);
        }
    }
}