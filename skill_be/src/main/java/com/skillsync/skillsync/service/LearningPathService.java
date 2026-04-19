package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.learningpath.LearningPathCreateRequest;
import com.skillsync.skillsync.dto.request.learningpath.LearningPathLessonRequest;
import com.skillsync.skillsync.dto.request.learningpath.LearningPathModuleRequest;
import com.skillsync.skillsync.dto.response.learningpath.LearningPathEnrollResponse;
import com.skillsync.skillsync.dto.response.learningpath.LearningPathLessonResponse;
import com.skillsync.skillsync.dto.response.learningpath.LearningPathModuleResponse;
import com.skillsync.skillsync.dto.response.learningpath.LearningPathResponse;
import com.skillsync.skillsync.entity.LearningPath;
import com.skillsync.skillsync.entity.LearningPathEnrollment;
import com.skillsync.skillsync.entity.LearningPathLesson;
import com.skillsync.skillsync.entity.LearningPathModule;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.LearningPathStatus;
import com.skillsync.skillsync.enums.RegistrationType;
import com.skillsync.skillsync.enums.Role;
import com.skillsync.skillsync.repository.LearningPathEnrollmentRepository;
import com.skillsync.skillsync.repository.LearningPathRepository;
import com.skillsync.skillsync.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LearningPathService {

    private final LearningPathRepository learningPathRepository;
    private final LearningPathEnrollmentRepository learningPathEnrollmentRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    @PersistenceContext
    private EntityManager entityManager;

    /** GET all — Admin can see all; returns all sorted by newest */
    @Transactional(readOnly = true)
    public List<LearningPathResponse> getAll() {
        return learningPathRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    /** GET approved — public explore page */
    @Transactional(readOnly = true)
    public List<LearningPathResponse> getApproved() {
        return learningPathRepository.findByStatusOrderByCreatedAtDesc(LearningPathStatus.APPROVED)
                .stream().map(this::toResponseSummary).toList();
    }

    /** GET my paths — mentor sees their own */
    @Transactional(readOnly = true)
    public List<LearningPathResponse> getMyPaths() {
        User user = userService.getCurrentUser();
        return learningPathRepository.findByTeacherIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    /** GET enrolled paths — learner sees enrolled paths from DB */
    @Transactional(readOnly = true)
    public List<LearningPathResponse> getEnrolledPaths() {
        User user = userService.getCurrentUser();
        return learningPathEnrollmentRepository.findByStudentIdOrderByEnrolledAtDesc(user.getId())
                .stream()
                .map(e -> {
                    LearningPathResponse base = toResponseSummary(e.getLearningPath());
                    base.setProgressPercent(e.getProgressPercent() != null ? e.getProgressPercent() : 0);
                    base.setEnrollmentStatus(e.getStatus());
                    return base;
                })
                .toList();
    }

    /** GET by id */
    @Transactional(readOnly = true)
    public LearningPathResponse getById(UUID id) {
        LearningPath lp = learningPathRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lộ trình không tồn tại"));
        return toResponse(lp);
    }

    /** POST — create new learning path */
    @Transactional
    public LearningPathResponse create(UUID mentorId, LearningPathCreateRequest req) {
        User teacher = userRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor không tồn tại: " + mentorId));

        LearningPath lp = LearningPath.builder()
                .teacher(teacher)
                .title(req.getTitle())
                .shortDescription(req.getShortDescription())
                .description(req.getDescription())
                .category(req.getCategory())
                .level(req.getLevel())
                .duration(req.getDuration())
                .emoji(req.getEmoji() != null ? req.getEmoji() : "📚")
                .thumbnailUrl(req.getThumbnailUrl())
                .totalCredits(req.getTotalCredits() != null ? req.getTotalCredits() : 0)
                .maxStudents(req.getMaxStudents() != null ? req.getMaxStudents() : 0)
                .registrationType(req.getRegistrationType() != null ? req.getRegistrationType() : RegistrationType.AUTO)
                .status(teacher.getRole() == Role.ADMIN ? LearningPathStatus.APPROVED : LearningPathStatus.PENDING)
                .modules(new ArrayList<>())
                .enrollments(new ArrayList<>())
                .build();

        // Save first to get the id
        LearningPath saved = learningPathRepository.save(lp);

        // Build modules and lessons
        if (req.getModules() != null) {
            int moduleOrder = 0;
            for (LearningPathModuleRequest modReq : req.getModules()) {
                LearningPathModule module = LearningPathModule.builder()
                        .learningPath(saved)
                        .title(modReq.getTitle() != null ? modReq.getTitle() : "Module")
                        .description(modReq.getDescription())
                        .objective(modReq.getObjective())
                        .orderIndex(moduleOrder++)
                        .enableSupport(modReq.getEnableSupport() != null ? modReq.getEnableSupport() : false)
                        .hasQuiz(modReq.getHasQuiz() != null ? modReq.getHasQuiz() : false)
                        .isQuizMandatory(modReq.getIsQuizMandatory() != null ? modReq.getIsQuizMandatory() : false)
                        .sessionsNeeded(modReq.getSessionsNeeded() != null ? modReq.getSessionsNeeded() : 0)
                        .lessons(new ArrayList<>())
                        .build();


                if (modReq.getLessons() != null) {
                    int lessonOrder = 0;
                    for (LearningPathLessonRequest lesReq : modReq.getLessons()) {
                        LearningPathLesson lesson = LearningPathLesson.builder()
                                .module(module)
                                .title(lesReq.getTitle() != null ? lesReq.getTitle() : "Bài học")
                                .description(lesReq.getDescription())
                                .videoUrl(lesReq.getVideoUrl())
                                .durationMinutes(lesReq.getDurationMinutes())
                                .isPreview(lesReq.getIsPreview() != null ? lesReq.getIsPreview() : false)
                                .orderIndex(lessonOrder++)
                                .build();
                        module.getLessons().add(lesson);
                    }
                }
                saved.getModules().add(module);
            }
        }

        LearningPath result = learningPathRepository.save(saved);
        return toResponse(result);
    }

    /** PATCH admin approve */
    @Transactional
    public LearningPathResponse approve(UUID id) {
        LearningPath lp = learningPathRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lộ trình không tồn tại"));
        lp.setStatus(LearningPathStatus.APPROVED);
        lp.setRejectionReason(null);
        return toResponse(learningPathRepository.save(lp));
    }

    /** PATCH admin reject */
    @Transactional
    public LearningPathResponse reject(UUID id, String reason) {
        LearningPath lp = learningPathRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lộ trình không tồn tại"));
        lp.setStatus(LearningPathStatus.REJECTED);
        lp.setRejectionReason(reason);
        return toResponse(learningPathRepository.save(lp));
    }

    /** POST enroll current user into approved path */
    @Transactional
    public LearningPathEnrollResponse enroll(UUID learningPathId) {
        User student = userService.getCurrentUser();
        LearningPath lp = learningPathRepository.findById(learningPathId)
                .orElseThrow(() -> new RuntimeException("Lộ trình không tồn tại"));

        if (lp.getStatus() != LearningPathStatus.APPROVED) {
            throw new RuntimeException("Lộ trình chưa sẵn sàng để đăng ký");
        }

        var existing = learningPathEnrollmentRepository.findByLearningPathIdAndStudentId(lp.getId(), student.getId());
        if (existing.isPresent()) {
            return LearningPathEnrollResponse.builder()
                    .enrolled(true)
                    .enrollmentId(existing.get().getId().toString())
                    .creditsBalance(student.getCreditsBalance())
                    .message("Bạn đã đăng ký lộ trình này trước đó")
                    .build();
        }

        int cost = lp.getTotalCredits() != null ? lp.getTotalCredits() : 0;
        int balance = student.getCreditsBalance() != null ? student.getCreditsBalance() : 0;
        if (cost > balance) {
            throw new RuntimeException("Không đủ credits để đăng ký lộ trình này");
        }

        if (cost > 0) {
            student.setCreditsBalance(balance - cost);
            userRepository.save(student);
        }

        LearningPathEnrollment enrollment = LearningPathEnrollment.builder()
                .learningPath(lp)
                .student(student)
                .learnerId(student.getId())
                .progressPercent(0)
                .status(resolveEnrollmentStatus())
                .build();
        LearningPathEnrollment saved = learningPathEnrollmentRepository.save(enrollment);

        return LearningPathEnrollResponse.builder()
                .enrolled(true)
                .enrollmentId(saved.getId().toString())
                .creditsBalance(student.getCreditsBalance())
                .message("Đăng ký lộ trình thành công")
                .build();
    }

    /**
     * Đọc check-constraint status từ PostgreSQL để chọn giá trị hợp lệ theo DB hiện tại.
     * Tránh hard-code khiến lỗi khi schema của môi trường khác nhau.
     */
    private String resolveEnrollmentStatus() {
        try {
            Object defObj = entityManager.createNativeQuery("""
                    SELECT pg_get_constraintdef(c.oid)
                    FROM pg_constraint c
                    JOIN pg_class t ON c.conrelid = t.oid
                    WHERE t.relname = 'learning_path_enrollments'
                      AND c.conname = 'learning_path_enrollments_status_check'
                    """).getSingleResult();

            if (defObj == null) return "ENROLLED";
            String def = defObj.toString();

            Matcher m = Pattern.compile("'([^']+)'").matcher(def);
            List<String> allowed = new ArrayList<>();
            while (m.find()) {
                allowed.add(m.group(1));
            }
            if (allowed.isEmpty()) return "ENROLLED";

            List<String> preferred = List.of("ENROLLED", "ACTIVE", "IN_PROGRESS", "ONGOING", "PENDING");
            for (String candidate : preferred) {
                if (allowed.contains(candidate)) return candidate;
            }
            return allowed.get(0);
        } catch (Exception ignored) {
            return "ENROLLED";
        }
    }

    // ─── Mappers ────────────────────────────────────────────

    private LearningPathResponse toResponse(LearningPath lp) {
        List<LearningPathModuleResponse> moduleDtos = lp.getModules() == null ? List.of() :
                lp.getModules().stream().map(this::toModuleResponse).toList();

        int lessonCount = moduleDtos.stream().mapToInt(m -> m.getLessonCount() != null ? m.getLessonCount() : 0).sum();

        return LearningPathResponse.builder()
                .id(lp.getId().toString())
                .title(lp.getTitle())
                .shortDescription(lp.getShortDescription())
                .description(lp.getDescription())
                .category(lp.getCategory() != null ? lp.getCategory().name() : null)
                .level(lp.getLevel() != null ? lp.getLevel().name() : null)
                .duration(lp.getDuration())
                .emoji(lp.getEmoji())
                .thumbnailUrl(lp.getThumbnailUrl())
                .totalCredits(lp.getTotalCredits())
                .maxStudents(lp.getMaxStudents())
                .registrationType(lp.getRegistrationType() != null ? lp.getRegistrationType().name() : null)
                .status(lp.getStatus() != null ? lp.getStatus().name() : null)
                .rejectionReason(lp.getRejectionReason())
                .teacherId(lp.getTeacher().getId().toString())
                .teacherName(lp.getTeacher().getFullName())
                .teacherAvatarUrl(lp.getTeacher().getAvatarUrl())
                .teacherRole(lp.getTeacher().getRole() != null ? lp.getTeacher().getRole().name() : null)
                .teacherBio(lp.getTeacher().getBio())
                .moduleCount(lp.getModules() != null ? lp.getModules().size() : 0)
                .lessonCount(lessonCount)
                .enrollmentCount(lp.getEnrollments() != null ? lp.getEnrollments().size() : 0)
                .modules(moduleDtos)
                .createdAt(lp.getCreatedAt())
                .updatedAt(lp.getUpdatedAt())
                .build();
    }

    /** Summary (no modules list) for Explore page */
    private LearningPathResponse toResponseSummary(LearningPath lp) {
        int moduleCount = lp.getModules() != null ? lp.getModules().size() : 0;
        int lessonCount = lp.getModules() == null ? 0 :
                lp.getModules().stream().mapToInt(m -> m.getLessons() != null ? m.getLessons().size() : 0).sum();

        return LearningPathResponse.builder()
                .id(lp.getId().toString())
                .title(lp.getTitle())
                .shortDescription(lp.getShortDescription())
                .description(lp.getDescription())
                .category(lp.getCategory() != null ? lp.getCategory().name() : null)
                .level(lp.getLevel() != null ? lp.getLevel().name() : null)
                .duration(lp.getDuration())
                .emoji(lp.getEmoji())
                .thumbnailUrl(lp.getThumbnailUrl())
                .totalCredits(lp.getTotalCredits())
                .status(lp.getStatus() != null ? lp.getStatus().name() : null)
                .teacherId(lp.getTeacher().getId().toString())
                .teacherName(lp.getTeacher().getFullName())
                .teacherAvatarUrl(lp.getTeacher().getAvatarUrl())
                .teacherRole(lp.getTeacher().getRole() != null ? lp.getTeacher().getRole().name() : null)
                .moduleCount(moduleCount)
                .lessonCount(lessonCount)
                .enrollmentCount(lp.getEnrollments() != null ? lp.getEnrollments().size() : 0)
                .createdAt(lp.getCreatedAt())
                .build();
    }

    private LearningPathModuleResponse toModuleResponse(LearningPathModule m) {
        List<LearningPathLessonResponse> lessons = m.getLessons() == null ? List.of() :
                m.getLessons().stream().map(this::toLessonResponse).toList();
        return LearningPathModuleResponse.builder()
                .id(m.getId().toString())
                .title(m.getTitle())
                .description(m.getDescription())
                .objective(m.getObjective())
                .orderIndex(m.getOrderIndex())
                .enableSupport(m.getEnableSupport())
                .hasQuiz(m.getHasQuiz())
                .isQuizMandatory(m.getIsQuizMandatory())
                .lessons(lessons)

                .lessonCount(lessons.size())
                .build();
    }

    private LearningPathLessonResponse toLessonResponse(LearningPathLesson l) {
        return LearningPathLessonResponse.builder()
                .id(l.getId().toString())
                .title(l.getTitle())
                .description(l.getDescription())
                .videoUrl(l.getVideoUrl())
                .durationMinutes(l.getDurationMinutes())
                .isPreview(l.getIsPreview())
                .orderIndex(l.getOrderIndex())
                .build();
    }
}
