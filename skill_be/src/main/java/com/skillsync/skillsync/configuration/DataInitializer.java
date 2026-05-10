package com.skillsync.skillsync.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.skillsync.entity.*;
import com.skillsync.skillsync.enums.*;
import com.skillsync.skillsync.repository.*;
import com.skillsync.skillsync.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

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
    private final LearningPathRepository learningPathRepository;
    private final LearningPathEnrollmentRepository learningPathEnrollmentRepository;
    private final LeaderboardService leaderboardService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        
        // 1. Users
        seedUser("admin@skillsync.com", "Admin@123", Role.ADMIN, "System Admin");
        seedUser("user@skillsync.com",  "User@123",  Role.USER,  "Standard User");
        seedSampleUsers();

        // 2. Base Data
        seedSkills();
        seedTeachingSkills();
        seedForumCategories();
        backfillForumPostStatuses();
        seedMissions();

        // 3. Complex Data
        seedSystemLearningPaths();
        seedLeaderboard();
    }


    private void seedSampleUsers() {
        try (InputStream is = new ClassPathResource("seeds/sample_users.json").getInputStream()) {
            List<Map<String, String>> userDefs = objectMapper.readValue(is, new TypeReference<>() {});
            userDefs.forEach(def -> seedUser(def.get("email"), "User@123", Role.USER, def.get("fullName")));
        } catch (Exception e) {
            log.error("Failed to seed sample users: {}", e.getMessage());
        }
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
            log.info("Seeded {} user: {}", role.name(), email);
        }
    }

    private static String deriveFullNameFromEmail(String email) {
        if (email == null || !email.contains("@")) return "User";
        String local = email.split("@")[0];
        return Character.toUpperCase(local.charAt(0)) + local.substring(1);
    }

    private void seedSkills() {
        try (InputStream is = new ClassPathResource("seeds/skills.json").getInputStream()) {
            List<Map<String, String>> skillDefs = objectMapper.readValue(is, new TypeReference<>() {});
            int seeded = 0;
            for (Map<String, String> def : skillDefs) {
                if (!skillRepository.existsByName(def.get("name"))) {
                    skillRepository.save(Skill.builder()
                            .name(def.get("name"))
                            .category(SkillCategory.valueOf(def.get("category")))
                            .icon(def.get("icon"))
                            .build());
                    seeded++;
                }
            }
            if (seeded > 0) log.info("Seeded {} skills", seeded);
        } catch (Exception e) {
            log.error("Failed to seed skills: {}", e.getMessage());
        }
    }

    private void seedTeachingSkills() {
        if (userTeachingSkillRepository.count() > 0) return;
        List<User> users = userRepository.findAll().stream().filter(u -> u.getRole() == Role.USER).toList();
        List<Skill> allSkills = skillRepository.findAll();
        if (allSkills.isEmpty() || users.isEmpty()) return;

        Random random = new Random();
        int seededCount = 0;
        for (User user : users) {
            List<Skill> shuffledSkills = new ArrayList<>(allSkills);
            Collections.shuffle(shuffledSkills);
            int skillsToTeach = 1 + random.nextInt(3);
            for (int i = 0; i < skillsToTeach; i++) {
                Skill skill = shuffledSkills.get(i);
                userTeachingSkillRepository.save(UserTeachingSkill.builder()
                        .user(user)
                        .skill(skill)
                        .level(SkillLevel.values()[random.nextInt(SkillLevel.values().length)])
                        .experienceDesc("Kinh nghiệm thực tế giảng dạy " + skill.getName())
                        .outcomeDesc("Nắm vững " + skill.getName())
                        .teachingStyle("Thực hành hands-on")
                        .creditsPerHour(10 + random.nextInt(41))
                        .verificationStatus(VerificationStatus.APPROVED)
                        .build());
                seededCount++;
            }
        }
        log.info("Seeded {} teaching skills", seededCount);
    }

    private void seedForumCategories() {
        try (InputStream is = new ClassPathResource("seeds/forum_categories.json").getInputStream()) {
            List<ForumCategory> categories = objectMapper.readValue(is, new TypeReference<>() {});
            int updated = 0;
            for (ForumCategory cat : categories) {
                if (forumCategoryRepository.findByNameIgnoreCase(cat.getName()).isEmpty()) {
                    forumCategoryRepository.save(cat);
                    updated++;
                }
            }
            if (updated > 0) log.info("Seeded {} forum categories", updated);
        } catch (Exception e) {
            log.error("Failed to seed forum categories: {}", e.getMessage());
        }
    }

    private void backfillForumPostStatuses() {
        List<ForumPost> posts = forumPostRepository.findAll().stream().filter(p -> p.getStatus() == null).toList();
        if (!posts.isEmpty()) {
            posts.forEach(p -> p.setStatus(ForumPostStatus.APPROVED));
            forumPostRepository.saveAll(posts);
            log.info("Backfilled {} forum posts", posts.size());
        }
    }

    private void seedMissions() {
        if (creditMissionRepository.count() > 0) return;
        try (InputStream is = new ClassPathResource("seeds/missions.json").getInputStream()) {
            List<CreditMission> missions = objectMapper.readValue(is, new TypeReference<>() {});
            creditMissionRepository.saveAll(missions);
            log.info("Seeded {} default missions", missions.size());
        } catch (Exception e) {
            log.error("Failed to seed missions: {}", e.getMessage());
        }
    }

    private void seedSystemLearningPaths() {
        final String prefix = "SYS_SEED_";
        if (learningPathRepository.findAll().stream().anyMatch(lp -> lp.getTitle().startsWith(prefix))) return;

        User admin = userRepository.findByEmail("admin@skillsync.com").orElse(null);
        if (admin == null) return;

        try (InputStream is = new ClassPathResource("seeds/learning_paths.json").getInputStream()) {
            List<Map<String, Object>> pathDefs = objectMapper.readValue(is, new TypeReference<>() {});
            for (Map<String, Object> def : pathDefs) {
                LearningPath lp = LearningPath.builder()
                        .teacher(admin)
                        .title((String) def.get("title"))
                        .shortDescription((String) def.get("shortDescription"))
                        .description((String) def.get("shortDescription"))
                        .category(SkillCategory.valueOf((String) def.get("category")))
                        .level(SkillLevel.valueOf((String) def.get("level")))
                        .duration("6 tuần")
                        .emoji("📚")
                        .thumbnailUrl((String) def.get("thumbUrl"))
                        .totalCredits((Integer) def.get("totalCredits"))
                        .maxStudents(999)
                        .registrationType(RegistrationType.AUTO)
                        .status(LearningPathStatus.APPROVED)
                        .modules(new ArrayList<>())
                        .build();

                LearningPath saved = learningPathRepository.save(lp);
                List<Map<String, Object>> moduleDefs = (List<Map<String, Object>>) def.get("modules");
                
                for (int mi = 0; mi < moduleDefs.size(); mi++) {
                    Map<String, Object> mDef = moduleDefs.get(mi);
                    LearningPathModule module = LearningPathModule.builder()
                            .learningPath(saved)
                            .title((String) mDef.get("title"))
                            .orderIndex(mi)
                            .enableSupport((Boolean) mDef.get("enableSupport"))
                            .hasQuiz((Boolean) mDef.get("hasQuiz"))
                            .lessons(new ArrayList<>())
                            .build();

                    List<Map<String, Object>> lessonDefs = (List<Map<String, Object>>) mDef.get("lessons");
                    for (int li = 0; li < lessonDefs.size(); li++) {
                        Map<String, Object> lDef = lessonDefs.get(li);
                        module.getLessons().add(LearningPathLesson.builder()
                                .module(module)
                                .title((String) lDef.get("title"))
                                .videoUrl((String) lDef.get("videoUrl"))
                                .durationMinutes((Integer) lDef.get("durationMinutes"))
                                .isPreview((Boolean) lDef.get("isPreview"))
                                .orderIndex(li)
                                .build());
                    }
                    saved.getModules().add(module);
                }
                learningPathRepository.save(saved);
            }
            log.info("Seeded system learning paths");
            autoEnrollUser(prefix);
        } catch (Exception e) {
            log.error("Failed to seed learning paths: {}", e.getMessage());
        }
    }

    private void autoEnrollUser(String prefix) {
        User student = userRepository.findByEmail("user@skillsync.com").orElse(null);
        if (student == null) return;

        student.setCreditsBalance(Math.max(student.getCreditsBalance(), 1000));
        userRepository.save(student);

        List<LearningPath> paths = learningPathRepository.findAll().stream()
                .filter(lp -> lp.getTitle().startsWith(prefix)).toList();

        int count = 0;
        for (LearningPath lp : paths) {
            if (!learningPathEnrollmentRepository.existsByLearningPathIdAndStudentId(lp.getId(), student.getId())) {
                int cost = lp.getTotalCredits() != null ? lp.getTotalCredits() : 0;
                if (student.getCreditsBalance() >= cost) {
                    student.setCreditsBalance(student.getCreditsBalance() - cost);
                    userRepository.save(student);
                    learningPathEnrollmentRepository.save(LearningPathEnrollment.builder()
                            .learningPath(lp).student(student).learnerId(student.getId())
                            .progressPercent(0).status("ENROLLED").build());
                    count++;
                }
            }
        }
        log.info("Auto-enrolled {} paths for user@skillsync.com", count);
    }

    private void seedLeaderboard() {
        Map<String, Double> topUsers = Map.of(
            "nguyenvana@gmail.com", 150000.0,
            "tranthib@gmail.com", 125000.0
        );
        topUsers.forEach((email, score) -> 
            userRepository.findByEmail(email).ifPresent(u -> {
                leaderboardService.incrementScore(u.getId().toString(), score);
                log.info("Set leaderboard score for {}: {}", email, score);
            })
        );
    }
}
