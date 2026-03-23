package com.skillsync.skillsync.configuration;

import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.Role;
import com.skillsync.skillsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser("admin@skillsync.com", "Admin@123", Role.ADMIN);
        seedUser("user@skillsync.com",  "User@123",  Role.USER);
    }

    private void seedUser(String email, String rawPassword, Role role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            String fullName = deriveFullNameFromEmail(email);
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .fullName(fullName)
                    .build();
            userRepository.save(user);
            log.info("✅ Seeded {} user: {}", role.name(), email);
        } else {
            log.info("⏩ {} user already exists: {}", role.name(), email);
        }
    }

    private static String deriveFullNameFromEmail(String email) {
        if (email == null || email.isBlank()) return "User";
        String local = email.split("@")[0];
        if (local.isBlank()) return "User";
        return Character.toUpperCase(local.charAt(0)) + local.substring(1);
    }
}
