package com.skillsync.skillsync.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.skillsync.entity.Skill;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.entity.ForumCategory;
import com.skillsync.skillsync.entity.ForumPost;
import com.skillsync.skillsync.enums.Role;
import com.skillsync.skillsync.enums.ForumPostStatus;
import com.skillsync.skillsync.enums.SkillCategory;

import com.skillsync.skillsync.entity.CreditMission;
import com.skillsync.skillsync.enums.MissionType;
import com.skillsync.skillsync.repository.*;

import com.skillsync.skillsync.entity.UserTeachingSkill;
import com.skillsync.skillsync.enums.VerificationStatus;
import com.skillsync.skillsync.enums.SkillLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SkillRepository skillRepository;
    private final CreditMissionRepository creditMissionRepository;
    private final ForumCategoryRepository forumCategoryRepository;
    private final ForumPostRepository forumPostRepository;
    private final UserTeachingSkillRepository userTeachingSkillRepository;

    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        seedUser("admin@skillsync.com", "Admin@123", Role.ADMIN, "System Admin");
        seedUser("user@skillsync.com",  "User@123",  Role.USER,  "Standard User");

        // Danh sách 10 user mẫu
        seedUser("nguyenvana@gmail.com", "User@123", Role.USER, "Nguyễn Văn An");
        seedUser("tranthib@gmail.com", "User@123", Role.USER, "Trần Thị Bình");
        seedUser("lehoangc@gmail.com", "User@123", Role.USER, "Lê Hoàng Cường");
        seedUser("phamduyd@gmail.com", "User@123", Role.USER, "Phạm Duy Đạt");
        seedUser("vuongthie@gmail.com", "User@123", Role.USER, "Vương Thị Yến");
        seedUser("dangquangf@gmail.com", "User@123", Role.USER, "Đặng Quang Phúc");
        seedUser("buitrangg@gmail.com", "User@123", Role.USER, "Bùi Trang Giang");
        seedUser("ngominhh@gmail.com", "User@123", Role.USER, "Ngô Minh Hiếu");
        seedUser("lythanhk@gmail.com", "User@123", Role.USER, "Lý Thanh Kiên");
        seedUser("hotuanl@gmail.com", "User@123", Role.USER, "Hồ Tuấn Lộc");
        seedSkills();
        seedTeachingSkills(); // Thêm dữ liệu test AI
        seedForumCategories();
        backfillForumPostStatuses();
        seedMissions();

    }


    private void seedUser(String email, String rawPassword, Role role, String fullName) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .fullName(fullName != null ? fullName : deriveFullNameFromEmail(email))
                    .isEmailVerified(true)
                    .build();
            userRepository.save(user);
            log.info("✅ Seeded {} user: {}", role.name(), email);
        } else {
            log.info("⏩ {} user already exists: {}", role.name(), email);
        }
    }

    private void seedUser(String email, String rawPassword, Role role) {
        seedUser(email, rawPassword, role, null);
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

    // ─── Teaching Skills (AI Testing) ────────────────────────────────────────

    private void seedTeachingSkills() {
        if (userTeachingSkillRepository.count() > 0) {
            log.info("⏩ Teaching skills already seeded");
            return;
        }

        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.USER)
                .toList();
        List<Skill> allSkills = skillRepository.findAll();

        if (allSkills.isEmpty() || users.isEmpty()) return;

        Random random = new Random();
        int seededCount = 0;

        for (User user : users) {
            // Shuffle skills để chọn ngẫu nhiên
            List<Skill> shuffledSkills = new ArrayList<>(allSkills);
            Collections.shuffle(shuffledSkills);

            // Mỗi user dạy ngẫu nhiên 1 đến 3 kỹ năng
            int skillsToTeach = 1 + random.nextInt(3);

            for (int i = 0; i < skillsToTeach; i++) {
                Skill skill = shuffledSkills.get(i);
                
                // Mức độ ngẫu nhiên
                SkillLevel level = SkillLevel.values()[random.nextInt(SkillLevel.values().length)];
                
                UserTeachingSkill uts = UserTeachingSkill.builder()
                        .user(user)
                        .skill(skill)
                        .level(level)
                        .experienceDesc("Tôi có " + (random.nextInt(10) + 1) + " năm kinh nghiệm làm việc và giảng dạy " + skill.getName() + " trong môi trường thực tế. Từng hỗ trợ nhiều học viên đạt được mục tiêu.")
                        .outcomeDesc("Nắm vững nền tảng " + skill.getName() + "\nTự tin áp dụng vào thực tế\nHoàn thành project cá nhân")
                        .teachingStyle("Dạy theo hướng thực hành (hands-on), tập trung vào dự án thực tế.")
                        .creditsPerHour(10 + random.nextInt(41)) // 10 - 50 credits
                        .verificationStatus(VerificationStatus.APPROVED) // Đã duyệt để hiện lên AI/Explore
                        .hidden(false)
                        .build();

                userTeachingSkillRepository.save(uts);
                seededCount++;
            }
        }
        log.info("✅ Seeded {} teaching skills for AI testing", seededCount);
    }

    // ─── Forum Categories ───────────────────────────────────────────────────

    private void seedForumCategories() {
        List<ForumCategory> defaultCategories = List.of(
                ForumCategory.builder().name("Mẹo học tập").description("Mẹo, công cụ và phương pháp học hiệu quả.").icon("").displayOrder(1).build(),
                ForumCategory.builder().name("Gợi ý giáo viên").description("Đề xuất và tìm kiếm giáo viên phù hợp.").icon("").displayOrder(2).build(),
                ForumCategory.builder().name("Tài nguyên học tập").description("Tài liệu, roadmap và nguồn học tập hữu ích.").icon("").displayOrder(3).build(),
                ForumCategory.builder().name("Hỏi đáp").description("Đặt câu hỏi và thảo luận cùng cộng đồng.").icon("").displayOrder(4).build(),
                ForumCategory.builder().name("Chia sẻ kinh nghiệm").description("Chia sẻ câu chuyện, kết quả và trải nghiệm học tập.").icon("").displayOrder(5).build(),
                ForumCategory.builder().name("Thảo luận chung").description("Trao đổi, góp ý và thảo luận các chủ đề học tập.").icon("").displayOrder(6).build(),
                ForumCategory.builder().name("Kinh nghiệm học tập").description("Chia sẻ phương pháp và cách học hiệu quả.").icon("").displayOrder(7).build(),
                ForumCategory.builder().name("Tài liệu tham khảo").description("Chia sẻ tài liệu, link và nguồn học thêm hữu ích.").icon("").displayOrder(8).build()
        );

        List<ForumCategory> existingCategories = forumCategoryRepository.findAllByOrderByDisplayOrderAsc();
        int updated = 0;

        for (ForumCategory defaultCategory : defaultCategories) {
            ForumCategory target = existingCategories.stream()
                    .filter(category -> category.getDisplayOrder() != null
                            && category.getDisplayOrder().equals(defaultCategory.getDisplayOrder()))
                    .findFirst()
                    .orElseGet(() -> forumCategoryRepository.findByNameIgnoreCase(defaultCategory.getName()).orElse(null));

            if (target == null) {
                forumCategoryRepository.save(defaultCategory);
                updated++;
                continue;
            }

            boolean changed = false;
            if (!defaultCategory.getName().equals(target.getName())) {
                target.setName(defaultCategory.getName());
                changed = true;
            }
            if (!defaultCategory.getDescription().equals(target.getDescription())) {
                target.setDescription(defaultCategory.getDescription());
                changed = true;
            }
            if (!defaultCategory.getIcon().equals(target.getIcon())) {
                target.setIcon(defaultCategory.getIcon());
                changed = true;
            }
            if (!defaultCategory.getDisplayOrder().equals(target.getDisplayOrder())) {
                target.setDisplayOrder(defaultCategory.getDisplayOrder());
                changed = true;
            }

            if (changed) {
                forumCategoryRepository.save(target);
                updated++;
            }
        }

        if (updated > 0) {
            log.info("✅ Synchronized {} forum categories", updated);
        } else {
            log.info("⏩ Forum categories already synchronized");
        }
    }

    private void backfillForumPostStatuses() {
        try {
            List<ForumPost> posts = forumPostRepository.findAll();
            long updated = 0;
            for (ForumPost post : posts) {
                if (post.getStatus() == null) {
                    post.setStatus(ForumPostStatus.APPROVED);
                    updated++;
                }
            }
            if (updated > 0) {
                forumPostRepository.saveAll(posts);
                log.info("✅ Backfilled {} forum posts to APPROVED", updated);
            }
        } catch (Exception e) {
            log.warn("Could not backfill forum post statuses: {}", e.getMessage());
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
