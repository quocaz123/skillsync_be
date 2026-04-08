package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.review.ReviewRequest;
import com.skillsync.skillsync.entity.Review;
import com.skillsync.skillsync.entity.Session;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.SessionStatus;
import com.skillsync.skillsync.exception.AppException;
import com.skillsync.skillsync.exception.ErrorCode;
import com.skillsync.skillsync.repository.ReviewRepository;
import com.skillsync.skillsync.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final SessionRepository sessionRepository;
    private final UserService userService;
    private final SessionService sessionService;

    @Transactional
    public void createReview(ReviewRequest request) {
        User reviewer = userService.getCurrentUser();
        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (!session.getLearner().getId().equals(reviewer.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new AppException(ErrorCode.INVALID_REQUEST); // Must be completed to review
        }

        if (reviewRepository.existsBySessionIdAndReviewerId(session.getId(), reviewer.getId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST); // Already reviewed
        }

        User reviewee = session.getTeacher();

        Review review = Review.builder()
                .session(session)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        reviewRepository.save(review);
    }

    public java.util.List<com.skillsync.skillsync.dto.response.review.ReviewResponse> getReviewsByReviewee(java.util.UUID revieweeId) {
        return reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(revieweeId).stream()
                .map(r -> com.skillsync.skillsync.dto.response.review.ReviewResponse.builder()
                        .id(r.getId())
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt())
                        .reviewerId(r.getReviewer() != null ? r.getReviewer().getId() : null)
                        .reviewerName(r.getReviewer() != null ? r.getReviewer().getFullName() : null)
                        .reviewerAvatar(r.getReviewer() != null ? r.getReviewer().getAvatarUrl() : null)
                        .sessionId(r.getSession() != null ? r.getSession().getId() : null)
                        .skillName(r.getSession() != null && r.getSession().getTeachingSkill() != null ? r.getSession().getTeachingSkill().getSkill().getName() : null)
                        .build())
                .toList();
    }
}
