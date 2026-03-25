package com.skillsync.skillsync.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.skillsync.entity.Skill;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.Role;
import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.repository.SkillRepository;
import com.skillsync.skillsync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SkillRepository skillRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        seedUser("admin@skillsync.com", "Admin@123", Role.ADMIN);
        seedUser("user@skillsync.com",  "User@123",  Role.USER);
        seedSkills();
    }

    // ─── Users ────────────────────────────────────────────────────────────────

    private void seedUser(String email, String rawPassword, Role role) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .fullName(deriveFullNameFromEmail(email))
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

    // ─── Skills ───────────────────────────────────────────────────────────────

    private void seedSkills() {
        try {
            ClassPathResource resource = new ClassPathResource("seeds/skills.json");
            InputStream inputStream = resource.getInputStream();

            List<Map<String, String>> skillDefs =
                    objectMapper.readValue(inputStream, new TypeReference<>() {});

            int seeded = 0;
            for (Map<String, String> def : skillDefs) {
                String name = def.get("name");
                if (!skillRepository.existsByName(name)) {
                    skillRepository.save(Skill.builder()
                            .name(name)
                            .category(SkillCategory.valueOf(def.get("category")))
                            .icon(def.get("icon"))
                            .build());
                    seeded++;
                }
            }

            if (seeded > 0) log.info("✅ Seeded {} skills from skills.json", seeded);
            else            log.info("⏩ Skills already seeded");

        } catch (Exception e) {
            log.error("❌ Failed to seed skills from seeds/skills.json: {}", e.getMessage());
        }
    }
}
