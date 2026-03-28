package com.skillsync.skillsync.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.skillsync.entity.Skill;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.Role;
import com.skillsync.skillsync.enums.SkillCategory;
import com.skillsync.skillsync.entity.CreditMission;
import com.skillsync.skillsync.enums.MissionType;
import com.skillsync.skillsync.repository.CreditMissionRepository;
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
    private final CreditMissionRepository creditMissionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        seedUser("admin@skillsync.com", "Admin@123", Role.ADMIN);
        seedUser("user@skillsync.com",  "User@123",  Role.USER);
        seedSkills();
        seedMissions();
    }


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

            if (seeded > 0) log.info("Seeded {} skills from skills.json", seeded);
            else            log.info("Skills already seeded");

        } catch (Exception e) {
            log.error("Failed to seed skills from seeds/skills.json: {}", e.getMessage());
        }
    }

    // ─── Missions ─────────────────────────────────────────────────────────────
    
    private void seedMissions() {
        if (creditMissionRepository.count() > 0) {
            log.info("⏩ Missions already seeded");
            return;
        }

        List<CreditMission> defaultMissions = List.of(
            CreditMission.builder().title("Đăng nhập hằng ngày").description("Đăng nhập vào hệ thống để nhận thưởng mỗi ngày.").rewardAmount(10).missionType(MissionType.DAILY).targetAction("LOGIN").build(),
            CreditMission.builder().title("Tham gia 1 buổi học").description("Hoàn thành ít nhất một buổi học bất kỳ trong ngày.").rewardAmount(30).missionType(MissionType.DAILY).targetAction("JOIN_SESSION").build(),
            CreditMission.builder().title("Chia sẻ khóa học").description("Chia sẻ một khóa học lên mạng xã hội để lan tỏa kiến thức.").rewardAmount(20).missionType(MissionType.DAILY).targetAction("SHARE_COURSE").build(),
            CreditMission.builder().title("Online 30 phút").description("Hoạt động trên hệ thống đủ 30 phút trong ngày.").rewardAmount(50).missionType(MissionType.DAILY).targetAction("ONLINE_30_MINS").build(),
            CreditMission.builder().title("Cập nhật hồ sơ").description("Hoàn thiện thông tin cá nhân của bạn để mọi người có thể biết đến bạn nhiều hơn.").rewardAmount(50).missionType(MissionType.ONCE).targetAction("UPDATE_PROFILE").build(),
            CreditMission.builder().title("Tham gia buổi học đầu tiên").description("Đăng ký và hoàn thành trọn vẹn một buổi học do người khác tổ chức.").rewardAmount(100).missionType(MissionType.ONCE).targetAction("FIRST_SESSION_JOINED").build(),
            CreditMission.builder().title("Mở lớp dạy đầu tiên").description("Tạo và giảng dạy thành công một buổi học chia sẻ kỹ năng của bạn.").rewardAmount(200).missionType(MissionType.ONCE).targetAction("FIRST_SESSION_TAUGHT").build()
        );

        creditMissionRepository.saveAll(defaultMissions);
        log.info("✅ Seeded {} default missions", defaultMissions.size());
    }
}
