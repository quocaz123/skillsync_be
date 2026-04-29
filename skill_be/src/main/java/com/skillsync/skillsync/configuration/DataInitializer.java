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
import com.skillsync.skillsync.entity.LearningPath;
import com.skillsync.skillsync.entity.LearningPathEnrollment;
import com.skillsync.skillsync.entity.LearningPathLesson;
import com.skillsync.skillsync.entity.LearningPathModule;
import com.skillsync.skillsync.enums.LearningPathStatus;
import com.skillsync.skillsync.enums.RegistrationType;
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
    private final LearningPathRepository learningPathRepository;
    private final LearningPathEnrollmentRepository learningPathEnrollmentRepository;
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
        seedSystemLearningPaths(); // Seed 10 lộ trình hệ thống cho trang Explore
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

    // ─── Learning Paths (System) ─────────────────────────────────────────

    /**
     * Seed 10 learning paths hệ thống (teacher = admin) để FE hiển thị sẵn khi chạy.
     *
     * Idempotent: nếu đã có path mang prefix seed thì bỏ qua.
     */
    private void seedSystemLearningPaths() {
        final String prefix = "SYS_SEED_";
        List<LearningPath> seededPaths = learningPathRepository.findAll().stream()
                .filter(lp -> lp.getTitle() != null && lp.getTitle().startsWith(prefix))
                .toList();

        var adminOpt = userRepository.findByEmail("admin@skillsync.com");
        if (adminOpt.isEmpty()) {
            log.warn("⚠️ Không tìm thấy admin@skillsync.com để seed system learning paths");
            return;
        }
        User admin = adminOpt.get();

        record LessonDef(String title, String videoUrl, int durationMinutes, boolean isPreview) {}
        record ModuleDef(String title, boolean enableSupport, boolean hasQuiz, List<LessonDef> lessons) {}
        record PathDef(String title, String shortDescription, SkillCategory category, SkillLevel level, int totalCredits, List<ModuleDef> modules) {}

        List<PathDef> defs = List.of(
                new PathDef("SYS_SEED_1 React Fundamentals", "React cho người mới bắt đầu", SkillCategory.TECH, SkillLevel.BEGINNER, 0,
                        List.of(
                                new ModuleDef("Module 1 - Intro React", false, false, List.of(
                                        new LessonDef("Bài 1 - Setup project", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", 10, true),
                                        new LessonDef("Bài 2 - Components cơ bản", "https://www.youtube.com/watch?v=3JZ_D3ELwOQ", 12, true)
                                )),
                                new ModuleDef("Module 2 - State & Props", true, false, List.of(
                                        new LessonDef("Bài 1 - Props", "https://www.youtube.com/watch?v=0KSOMA3QBU", 11, false),
                                        new LessonDef("Bài 2 - State", "https://www.youtube.com/watch?v=tAGnKpE4NCI", 14, false)
                                ))
                        )
                ),
                new PathDef("SYS_SEED_2 JavaScript cho người đi làm", "Nắm vững JavaScript thực chiến", SkillCategory.TECH, SkillLevel.INTERMEDIATE, 20,
                        List.of(
                                new ModuleDef("Module 1 - ES Modules", false, false, List.of(
                                        new LessonDef("Bài 1 - Import/Export", "https://www.youtube.com/watch?v=YQHsXMglC9A", 10, true),
                                        new LessonDef("Bài 2 - Async/Await", "https://www.youtube.com/watch?v=2Z4G-8vKkYI", 15, true)
                                )),
                                new ModuleDef("Module 2 - Patterns", true, false, List.of(
                                        new LessonDef("Bài 1 - Closures", "https://www.youtube.com/watch?v=aqz-KE-bpKQ", 12, false),
                                        new LessonDef("Bài 2 - Design patterns", "https://www.youtube.com/watch?v=J---aiyznGQ", 16, false)
                                ))
                        )
                ),
                new PathDef("SYS_SEED_3 SQL từ con số 0", "SQL nền tảng để phân tích dữ liệu", SkillCategory.TECH, SkillLevel.BEGINNER, 0,
                        List.of(
                                new ModuleDef("Module 1 - SELECT & JOIN", false, false, List.of(
                                        new LessonDef("Bài 1 - SELECT", "https://www.youtube.com/watch?v=HXV3zeQKqUE", 10, true),
                                        new LessonDef("Bài 2 - JOIN", "https://www.youtube.com/watch?v=9InZQeQ6Qk8", 12, true)
                                )),
                                new ModuleDef("Module 2 - Aggregation", false, true, List.of(
                                        new LessonDef("Bài 1 - GROUP BY", "https://www.youtube.com/watch?v=RiOv1vG2K2s", 11, false),
                                        new LessonDef("Bài 2 - Window functions", "https://www.youtube.com/watch?v=GNq9v8n9b5k", 14, false)
                                ))
                        )
                ),
                new PathDef("SYS_SEED_4 UI/UX cơ bản với Figma", "Từ wireframe đến prototype", SkillCategory.DESIGN, SkillLevel.BEGINNER, 0,
                        List.of(
                                new ModuleDef("Module 1 - Figma basics", false, false, List.of(
                                        new LessonDef("Bài 1 - Frames", "https://www.youtube.com/watch?v=ysz5S6PUM-U", 10, true),
                                        new LessonDef("Bài 2 - Components", "https://www.youtube.com/watch?v=aqz-KE-bpKQ", 12, true)
                                )),
                                new ModuleDef("Module 2 - Prototype", true, false, List.of(
                                        new LessonDef("Bài 1 - Auto layout", "https://www.youtube.com/watch?v=9n3eaGFDqQ0", 11, false),
                                        new LessonDef("Bài 2 - Handoff", "https://www.youtube.com/watch?v=G3p9eG3Q4mI", 14, false)
                                ))
                        )
                ),
                new PathDef("SYS_SEED_5 Machine Learning nền tảng", "Tư duy ML và mô hình cơ bản", SkillCategory.DATA, SkillLevel.INTERMEDIATE, 30,
                        List.of(
                                new ModuleDef("Module 1 - Supervised learning", true, true, List.of(
                                        new LessonDef("Bài 1 - Regression", "https://www.youtube.com/watch?v=GmXwQx9G6fA", 10, true),
                                        new LessonDef("Bài 2 - Classification", "https://www.youtube.com/watch?v=aircAruvnKk", 15, true)
                                )),
                                new ModuleDef("Module 2 - Evaluation", true, false, List.of(
                                        new LessonDef("Bài 1 - Metrics", "https://www.youtube.com/watch?v=3cX5F5X0k8k", 12, false),
                                        new LessonDef("Bài 2 - Cross-validation", "https://www.youtube.com/watch?v=VbfpW5p2d9o", 16, false)
                                ))
                        )
                ),
                new PathDef("SYS_SEED_6 English for work", "Giao tiếp tiếng Anh trong môi trường công việc", SkillCategory.LANGUAGE, SkillLevel.INTERMEDIATE, 25,
                        List.of(
                                new ModuleDef("Module 1 - Email & meetings", false, false, List.of(
                                        new LessonDef("Bài 1 - Email templates", "https://www.youtube.com/watch?v=kJQP7kiw5Fk", 10, true),
                                        new LessonDef("Bài 2 - Meeting phrases", "https://www.youtube.com/watch?v=ScMzIvxBSi4", 15, true)
                                )),
                                new ModuleDef("Module 2 - Presentations", true, true, List.of(
                                        new LessonDef("Bài 1 - Structure slides", "https://www.youtube.com/watch?v=2Vv-BfVoq4g", 12, false),
                                        new LessonDef("Bài 2 - Q&A handling", "https://www.youtube.com/watch?v=hTWtf2vVfZs", 16, false)
                                ))
                        )
                ),
                new PathDef("SYS_SEED_7 Giao tiếp & thuyết trình", "Kỹ năng mềm để tự tin hơn", SkillCategory.SOFT_SKILL, SkillLevel.BEGINNER, 0,
                        List.of(
                                new ModuleDef("Module 1 - Body language", false, false, List.of(
                                        new LessonDef("Bài 1 - Eye contact", "https://www.youtube.com/watch?v=0hR2eZk6FqQ", 10, true),
                                        new LessonDef("Bài 2 - Voice control", "https://www.youtube.com/watch?v=CZgkq3p0vW4", 12, true)
                                )),
                                new ModuleDef("Module 2 - Storytelling", true, false, List.of(
                                        new LessonDef("Bài 1 - Open with hook", "https://www.youtube.com/watch?v=E7wJTI-1dvQ", 11, false),
                                        new LessonDef("Bài 2 - Closing strong", "https://www.youtube.com/watch?v=fLexgOxsZu0", 14, false)
                                ))
                        )
                ),
                new PathDef("SYS_SEED_8 Business Analysis essentials", "Phân tích yêu cầu và dữ liệu", SkillCategory.BUSINESS, SkillLevel.INTERMEDIATE, 35,
                        List.of(
                                new ModuleDef("Module 1 - Requirements", true, false, List.of(
                                        new LessonDef("Bài 1 - User stories", "https://www.youtube.com/watch?v=uel1gA4zqG8", 10, true),
                                        new LessonDef("Bài 2 - Acceptance criteria", "https://www.youtube.com/watch?v=7NOSDKb8w5M", 15, true)
                                )),
                                new ModuleDef("Module 2 - Data reasoning", true, true, List.of(
                                        new LessonDef("Bài 1 - Hypothesis", "https://www.youtube.com/watch?v=F2g5j7JYf7I", 12, false),
                                        new LessonDef("Bài 2 - Decision making", "https://www.youtube.com/watch?v=Zi_XLOBBiJY", 16, false)
                                ))
                        )
                ),
                new PathDef("SYS_SEED_9 Career & portfolio", "Xây portfolio để ứng tuyển hiệu quả", SkillCategory.CAREER, SkillLevel.BEGINNER, 0,
                        List.of(
                                new ModuleDef("Module 1 - Portfolio basics", false, false, List.of(
                                        new LessonDef("Bài 1 - Project selection", "https://www.youtube.com/watch?v=Z9T1o0cFv0Y", 10, true),
                                        new LessonDef("Bài 2 - Writing README", "https://www.youtube.com/watch?v=O6p86uwd7pI", 12, true)
                                )),
                                new ModuleDef("Module 2 - Demo & interview", true, false, List.of(
                                        new LessonDef("Bài 1 - Elevator pitch", "https://www.youtube.com/watch?v=Zi_XLOBBiJY", 11, false),
                                        new LessonDef("Bài 2 - Mock interview", "https://www.youtube.com/watch?v=uel1gA4zqG8", 14, false)
                                ))
                        )
                ),
                new PathDef("SYS_SEED_10 Advanced React patterns", "Tối ưu và nâng cấp kiến trúc", SkillCategory.TECH, SkillLevel.ADVANCED, 40,
                        List.of(
                                new ModuleDef("Module 1 - Performance", true, true, List.of(
                                        new LessonDef("Bài 1 - Memoization", "https://www.youtube.com/watch?v=1Leq2GJkJgE", 10, true),
                                        new LessonDef("Bài 2 - Rendering strategy", "https://www.youtube.com/watch?v=Y2hVQm5b1z8", 15, true)
                                )),
                                new ModuleDef("Module 2 - Architecture", true, false, List.of(
                                        new LessonDef("Bài 1 - Feature folders", "https://www.youtube.com/watch?v=J---aiyznGQ", 12, false),
                                        new LessonDef("Bài 2 - Testing approach", "https://www.youtube.com/watch?v=aqz-KE-bpKQ", 16, false)
                                ))
                        )
                )
        );

        if (seededPaths.isEmpty()) {
            int seeded = 0;
            for (PathDef def : defs) {
                LearningPath lp = LearningPath.builder()
                        .teacher(admin)
                        .title(def.title())
                        .shortDescription(def.shortDescription())
                        .description(def.shortDescription())
                        .category(def.category())
                        .level(def.level())
                        .duration("6 tuần")
                        .emoji("📚")
                        .thumbnailUrl(null)
                        .totalCredits(def.totalCredits())
                        .maxStudents(999)
                        .registrationType(RegistrationType.AUTO)
                        .status(LearningPathStatus.APPROVED)
                        .modules(new ArrayList<>())
                        .enrollments(new ArrayList<>())
                        .build();

                LearningPath saved = learningPathRepository.save(lp);

                List<LearningPathModule> modules = new ArrayList<>();
                for (int mi = 0; mi < def.modules().size(); mi++) {
                    ModuleDef m = def.modules().get(mi);
                    LearningPathModule module = LearningPathModule.builder()
                            .learningPath(saved)
                            .title(m.title())
                            .description("")
                            .objective("")
                            .orderIndex(mi)
                            .enableSupport(m.enableSupport())
                            .hasQuiz(m.hasQuiz())
                            .isQuizMandatory(false)
                            .sessionsNeeded(0)
                            .lessons(new ArrayList<>())
                            .build();

                    for (int li = 0; li < m.lessons().size(); li++) {
                        LessonDef l = m.lessons().get(li);
                        LearningPathLesson lesson = LearningPathLesson.builder()
                                .module(module)
                                .title(l.title())
                                .description("")
                                .videoUrl(l.videoUrl())
                                .durationMinutes(l.durationMinutes())
                                .isPreview(l.isPreview())
                                .orderIndex(li)
                                .build();
                        module.getLessons().add(lesson);
                    }

                    modules.add(module);
                }

                saved.getModules().addAll(modules);
                learningPathRepository.save(saved);
                seeded++;
            }
            log.info("✅ Seeded {} system learning paths", seeded);
            seededPaths = learningPathRepository.findAll().stream()
                    .filter(lp -> lp.getTitle() != null && lp.getTitle().startsWith(prefix))
                    .toList();
        }

        // ─── Auto-enroll into seeded paths for user@skillsync.com ────────────
        // Mục tiêu: chạy lên là có sẵn 10 khóa để test "Đang học".
        var studentOpt = userRepository.findByEmail("user@skillsync.com");
        if (studentOpt.isEmpty()) {
            log.warn("⚠️ Không tìm thấy user@skillsync.com để auto-enroll");
            return;
        }
        User student = studentOpt.get();
        int desiredCredits = 1000;
        Integer balance = student.getCreditsBalance() != null ? student.getCreditsBalance() : 0;
        if (balance < desiredCredits) {
            student.setCreditsBalance(desiredCredits);
            userRepository.save(student);
        }

        int enrolledCount = 0;
        for (LearningPath lp : seededPaths) {
            if (learningPathEnrollmentRepository.existsByLearningPathIdAndStudentId(lp.getId(), student.getId())) {
                continue;
            }

            int cost = lp.getTotalCredits() != null ? lp.getTotalCredits() : 0;
            int currentBalance = student.getCreditsBalance() != null ? student.getCreditsBalance() : 0;
            if (cost > currentBalance) {
                log.info("⏩ Skip enroll '{}' (cost={} > balance={})", lp.getTitle(), cost, currentBalance);
                continue;
            }

            if (cost > 0) {
                student.setCreditsBalance(currentBalance - cost);
                userRepository.save(student);
            }

            LearningPathEnrollment enrollment = LearningPathEnrollment.builder()
                    .learningPath(lp)
                    .student(student)
                    .learnerId(student.getId())
                    .progressPercent(0)
                    .status("ENROLLED")
                    .build();
            learningPathEnrollmentRepository.save(enrollment);
            enrolledCount++;
        }

        log.info("✅ Auto-enrolled {} seeded paths for user@skillsync.com", enrolledCount);
    }

}
