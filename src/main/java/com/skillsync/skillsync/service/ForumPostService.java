package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.forum.CreateForumPostRequest;
import com.skillsync.skillsync.dto.request.forum.UpdateForumPostRequest;
import com.skillsync.skillsync.dto.request.forum.VerifyForumPostRequest;
import com.skillsync.skillsync.dto.event.forum.ForumPostChangedEvent;
import com.skillsync.skillsync.dto.response.forum.AdminForumPostResponse;
import com.skillsync.skillsync.dto.response.forum.ForumPostDetailResponse;
import com.skillsync.skillsync.dto.response.forum.ForumPostResponse;
import com.skillsync.skillsync.entity.ForumCategory;
import com.skillsync.skillsync.entity.ForumPost;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.ForumPostStatus;
import com.skillsync.skillsync.enums.PostType;
import com.skillsync.skillsync.enums.VoteType;
import com.skillsync.skillsync.repository.ForumCategoryRepository;
import com.skillsync.skillsync.repository.ForumPostRepository;
import com.skillsync.skillsync.repository.PostSaveRepository;
import com.skillsync.skillsync.repository.PostVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForumPostService {
    private final ForumPostRepository postRepository;
    private final ForumCategoryRepository categoryRepository;
    private final PostVoteRepository voteRepository;
    private final PostSaveRepository saveRepository;
    private final UserService userService;
    private final ForumCommentService commentService;
    private final ForumRealtimeEventService forumRealtimeEventService;

    public List<AdminForumPostResponse> getAdminPosts(ForumPostStatus status) {
        List<ForumPost> posts = status != null
                ? postRepository.findByStatusOrderByCreatedAtDesc(status)
                : postRepository.findAllByOrderByCreatedAtDesc(Pageable.unpaged()).getContent();

        return posts.stream().map(this::toAdminResponse).toList();
    }

    @Transactional
    public AdminForumPostResponse verifyPost(UUID postId, VerifyForumPostRequest request) {
        User admin = userService.getCurrentUser();
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        if (!"APPROVED".equalsIgnoreCase(request.getAction()) && !"REJECTED".equalsIgnoreCase(request.getAction())) {
            throw new IllegalArgumentException("action must be APPROVED or REJECTED");
        }
        if ("REJECTED".equalsIgnoreCase(request.getAction()) && (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {
            throw new IllegalArgumentException("rejectionReason is required when rejecting a post");
        }

        post.setStatus("APPROVED".equalsIgnoreCase(request.getAction())
                ? ForumPostStatus.APPROVED
                : ForumPostStatus.REJECTED);
        post.setReviewedBy(admin);
        post.setReviewedAt(java.time.LocalDateTime.now());
        post.setRejectionReason("REJECTED".equalsIgnoreCase(request.getAction()) ? request.getRejectionReason() : null);

        ForumPost saved = postRepository.save(post);
        forumRealtimeEventService.publishForumPostChangedEvent(
            ForumPostChangedEvent.builder()
                .action("VERIFY")
                .postId(saved.getId())
                .title(saved.getTitle())
                .status(saved.getStatus())
                .rejectionReason(saved.getRejectionReason())
                .reviewedAt(saved.getReviewedAt())
                .reviewedByEmail(saved.getReviewedBy() != null ? saved.getReviewedBy().getEmail() : null)
                .authorId(saved.getAuthor() != null ? saved.getAuthor().getId() : null)
                .categoryId(saved.getCategory() != null ? saved.getCategory().getId() : null)
                .build()
        );

        return toAdminResponse(saved);
    }

    /**
     * Get all posts with pagination
     */
    @Transactional(readOnly = true)
    public Page<ForumPostResponse> getAllPosts(UUID categoryId, String searchKeyword, Pageable pageable) {
        User currentUser = getCurrentUserOrNull();
        Page<ForumPost> posts = postRepository.searchByStatusAndCategoryAndKeyword(
                ForumPostStatus.APPROVED,
                categoryId,
                searchKeyword,
                pageable);

        List<ForumPostResponse> content = posts.getContent().stream()
            .map(post -> toResponseSafely(post, currentUser))
            .filter(java.util.Objects::nonNull)
            .toList();

        return new PageImpl<>(content, pageable, posts.getTotalElements());
    }

    /**
     * Get post by ID with comments
     */
    @Transactional(readOnly = true)
    public ForumPostDetailResponse getPostById(UUID postId) {
        ForumPost post = postRepository.findWithDetailsById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        User currentUser = getCurrentUserOrNull();
        if (!canViewPost(post, currentUser)) {
            throw new RuntimeException("Post not found with id: " + postId);
        }

        return toDetailResponse(post, currentUser);
    }

    /**
     * Create new post
     */
    @Transactional
    public ForumPostResponse createPost(CreateForumPostRequest request) {
        User author = userService.getCurrentUser();

        ForumCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));

        String tagsString = request.getTags() != null ? String.join(",", request.getTags()) : null;

        ForumPost post = ForumPost.builder()
                .author(author)
                .category(category)
                .title(request.getTitle())
                .content(request.getContent())
            .postType(request.getPostType() != null ? request.getPostType() : PostType.DISCUSSION)
                .tags(tagsString)
            .status(ForumPostStatus.PENDING)
            .rejectionReason(null)
            .reviewedBy(null)
            .reviewedAt(null)
                .solved(false)
                .build();

        ForumPost saved = postRepository.save(post);
        forumRealtimeEventService.publishForumPostChangedEvent(
            ForumPostChangedEvent.builder()
                .action("CREATE")
                .postId(saved.getId())
                .title(saved.getTitle())
                .status(saved.getStatus())
                .rejectionReason(saved.getRejectionReason())
                .reviewedAt(saved.getReviewedAt())
                .reviewedByEmail(saved.getReviewedBy() != null ? saved.getReviewedBy().getEmail() : null)
                .authorId(saved.getAuthor() != null ? saved.getAuthor().getId() : null)
                .categoryId(saved.getCategory() != null ? saved.getCategory().getId() : null)
                .build()
        );
        return toResponse(saved);
    }

    /**
     * Update post (only author or admin)
     */
    @Transactional
    public ForumPostResponse updatePost(UUID postId, UpdateForumPostRequest request) {
        User currentUser = userService.getCurrentUser();
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized: only author can update this post");
        }

        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
        }
        if (request.getCategoryId() != null) {
            ForumCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + request.getCategoryId()));
            post.setCategory(category);
        }
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getPostType() != null) {
            post.setPostType(request.getPostType());
        } else if (post.getPostType() == null) {
            post.setPostType(PostType.DISCUSSION);
        }
        if (request.getTags() != null) {
            post.setTags(String.join(",", request.getTags()));
        }
        if (request.getSolved() != null) {
            post.setSolved(request.getSolved());
        }

        post.setStatus(ForumPostStatus.PENDING);
        post.setRejectionReason(null);
        post.setReviewedBy(null);
        post.setReviewedAt(null);

        ForumPost updated = postRepository.save(post);
        forumRealtimeEventService.publishForumPostChangedEvent(
            ForumPostChangedEvent.builder()
                .action("UPDATE")
                .postId(updated.getId())
                .title(updated.getTitle())
                .status(updated.getStatus())
                .rejectionReason(updated.getRejectionReason())
                .reviewedAt(updated.getReviewedAt())
                .reviewedByEmail(updated.getReviewedBy() != null ? updated.getReviewedBy().getEmail() : null)
                .authorId(updated.getAuthor() != null ? updated.getAuthor().getId() : null)
                .categoryId(updated.getCategory() != null ? updated.getCategory().getId() : null)
                .build()
        );
        return toResponse(updated);
    }

    /**
     * Delete post (only author or admin)
     */
    @Transactional
    public void deletePost(UUID postId) {
        User currentUser = userService.getCurrentUser();
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized: only author can delete this post");
        }

        forumRealtimeEventService.publishForumPostChangedEvent(
            ForumPostChangedEvent.builder()
                .action("DELETE")
                .postId(post.getId())
                .title(post.getTitle())
                .status(post.getStatus())
                .rejectionReason(post.getRejectionReason())
                .reviewedAt(post.getReviewedAt())
                .reviewedByEmail(post.getReviewedBy() != null ? post.getReviewedBy().getEmail() : null)
                .authorId(post.getAuthor() != null ? post.getAuthor().getId() : null)
                .categoryId(post.getCategory() != null ? post.getCategory().getId() : null)
                .build()
        );

        postRepository.delete(post);
    }

    /**
     * Get trending posts (top 10 by comment count)
     */
    @Transactional(readOnly = true)
    public List<ForumPostResponse> getTrendingPosts(int limit) {
        User currentUser = getCurrentUserOrNull();
        return postRepository.findByStatusOrderByCreatedAtDesc(ForumPostStatus.APPROVED).stream()
            .map(post -> toResponseSafely(post, currentUser))
            .filter(java.util.Objects::nonNull)
            .sorted((left, right) -> {
                long leftComments = left.getCommentCount() != null ? left.getCommentCount() : 0L;
                long rightComments = right.getCommentCount() != null ? right.getCommentCount() : 0L;

                int commentCompare = Long.compare(rightComments, leftComments);
                if (commentCompare != 0) return commentCompare;

                long leftScore = (left.getUpvotes() != null ? left.getUpvotes() : 0L) * 2
                    + (left.getSaveCount() != null ? left.getSaveCount() : 0L);
                long rightScore = (right.getUpvotes() != null ? right.getUpvotes() : 0L) * 2
                    + (right.getSaveCount() != null ? right.getSaveCount() : 0L);

                int scoreCompare = Long.compare(rightScore, leftScore);
                if (scoreCompare != 0) return scoreCompare;

                if (left.getCreatedAt() == null && right.getCreatedAt() == null) return 0;
                if (left.getCreatedAt() == null) return 1;
                if (right.getCreatedAt() == null) return -1;
                return right.getCreatedAt().compareTo(left.getCreatedAt());
            })
            .limit(limit)
            .toList();
    }

    /**
     * Get user's posts
     */
    @Transactional(readOnly = true)
    public Page<ForumPostResponse> getUserPosts(UUID userId, Pageable pageable) {
        User currentUser = getCurrentUserOrNull();
        boolean ownProfile = currentUser != null && currentUser.getId().equals(userId);
        boolean adminView = currentUser != null && currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().name());

        Page<ForumPost> posts = (ownProfile || adminView)
                ? postRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable)
                : postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(userId, ForumPostStatus.APPROVED, pageable);

        List<ForumPostResponse> content = posts.getContent().stream()
            .map(post -> toResponseSafely(post, currentUser))
            .filter(java.util.Objects::nonNull)
            .toList();

        return new PageImpl<>(content, pageable, posts.getTotalElements());
    }

    /**
     * Convert entity to response DTO
     */
    private ForumPostResponse toResponse(ForumPost post) {
        return toResponse(post, null);
    }

    /**
     * Convert entity to response DTO (with current user for like/save status)
     */
    private ForumPostResponse toResponse(ForumPost post, User currentUser) {
        Long upvotes = voteRepository.countByPostIdAndVoteType(post.getId(), VoteType.UPVOTE);
        Long downvotes = voteRepository.countByPostIdAndVoteType(post.getId(), VoteType.DOWNVOTE);
        Long commentCount = commentService.getCommentCount(post.getId());
        Long saveCount = saveRepository.countByPostId(post.getId());

        Boolean liked = false;
        Boolean saved = false;

        if (currentUser != null) {
            liked = voteRepository.findByPostIdAndUserId(post.getId(), currentUser.getId())
                    .map(v -> v.getVoteType() == VoteType.UPVOTE)
                    .orElse(false);
            saved = saveRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId());
        }

        // Validate required relationships
        if (post.getAuthor() == null) {
            throw new IllegalStateException("Post author is null for post: " + post.getId());
        }
        if (post.getCategory() == null) {
            throw new IllegalStateException("Post category is null for post: " + post.getId());
        }

        User author = post.getAuthor();
        ForumCategory category = post.getCategory();

        List<String> tags = post.getTags() != null && !post.getTags().isEmpty()
                ? List.of(post.getTags().split(","))
                : List.of();

        return ForumPostResponse.builder()
                .id(post.getId())
                .authorId(author.getId())
                .authorName(author.getFullName())
                .authorRole(author.getRole() != null ? author.getRole().name() : "USER")
                .authorAvatar(author.getAvatarUrl())
                .categoryId(category.getId())
                .categoryName(category.getName())
                .title(post.getTitle())
                .content(post.getContent())
                .postType(post.getPostType())
                .tags(tags)
                .upvotes(upvotes)
                .downvotes(downvotes)
                .commentCount(commentCount)
                .saveCount(saveCount)
                .solved(post.getSolved())
                .liked(liked)
                .saved(saved)
                .status(post.getStatus())
                .rejectionReason(post.getRejectionReason())
                .reviewedAt(post.getReviewedAt())
                .reviewedByEmail(post.getReviewedBy() != null ? post.getReviewedBy().getEmail() : null)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private ForumPostResponse toResponseSafely(ForumPost post, User currentUser) {
        try {
            return toResponse(post, currentUser);
        } catch (Exception e) {
            return null;
        }
    }

            private AdminForumPostResponse toAdminResponse(ForumPost post) {
            Long upvotes = voteRepository.countByPostIdAndVoteType(post.getId(), VoteType.UPVOTE);
            Long downvotes = voteRepository.countByPostIdAndVoteType(post.getId(), VoteType.DOWNVOTE);
            Long commentCount = commentService.getCommentCount(post.getId());
            Long saveCount = saveRepository.countByPostId(post.getId());

            List<String> tags = post.getTags() != null && !post.getTags().isEmpty()
                ? List.of(post.getTags().split(","))
                : List.of();

            return AdminForumPostResponse.builder()
                .id(post.getId())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getFullName())
                .authorEmail(post.getAuthor().getEmail())
                .authorRole(post.getAuthor().getRole() != null ? post.getAuthor().getRole().name() : "USER")
                .authorAvatar(post.getAuthor().getAvatarUrl())
                .categoryId(post.getCategory().getId())
                .categoryName(post.getCategory().getName())
                .title(post.getTitle())
                .content(post.getContent())
                .postType(post.getPostType())
                .tags(tags)
                .status(post.getStatus())
                .rejectionReason(post.getRejectionReason())
                .reviewedAt(post.getReviewedAt())
                .reviewedByEmail(post.getReviewedBy() != null ? post.getReviewedBy().getEmail() : null)
                .solved(post.getSolved())
                .upvotes(upvotes)
                .downvotes(downvotes)
                .commentCount(commentCount)
                .saveCount(saveCount)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
            }

    /**
     * Convert entity to detail response with comments
     */
    private ForumPostDetailResponse toDetailResponse(ForumPost post) {
        return toDetailResponse(post, getCurrentUserOrNull());
    }

    private ForumPostDetailResponse toDetailResponse(ForumPost post, User currentUser) {

        Long upvotes = voteRepository.countByPostIdAndVoteType(post.getId(), VoteType.UPVOTE);
        Long downvotes = voteRepository.countByPostIdAndVoteType(post.getId(), VoteType.DOWNVOTE);
        Long commentCount = commentService.getCommentCount(post.getId());
        Long saveCount = saveRepository.countByPostId(post.getId());

        Boolean liked = false;
        Boolean saved = false;

        if (currentUser != null) {
            liked = voteRepository.findByPostIdAndUserId(post.getId(), currentUser.getId())
                    .map(v -> v.getVoteType() == VoteType.UPVOTE)
                    .orElse(false);
            saved = saveRepository.existsByPostIdAndUserId(post.getId(), currentUser.getId());
        }

        List<String> tags = post.getTags() != null && !post.getTags().isEmpty()
                ? List.of(post.getTags().split(","))
                : List.of();

        return ForumPostDetailResponse.builder()
                .id(post.getId())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getFullName())
                .authorRole(post.getAuthor().getRole() != null ? post.getAuthor().getRole().name() : "USER")
                .authorAvatar(post.getAuthor().getAvatarUrl())
                .categoryId(post.getCategory().getId())
                .categoryName(post.getCategory().getName())
                .title(post.getTitle())
                .content(post.getContent())
                .postType(post.getPostType())
                .tags(tags)
                .upvotes(upvotes)
                .downvotes(downvotes)
                .commentCount(commentCount)
                .saveCount(saveCount)
                .solved(post.getSolved())
                .liked(liked)
                .saved(saved)
                .comments(commentService.getPostComments(post.getId()))
                .status(post.getStatus())
                .rejectionReason(post.getRejectionReason())
                .reviewedAt(post.getReviewedAt())
                .reviewedByEmail(post.getReviewedBy() != null ? post.getReviewedBy().getEmail() : null)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private User getCurrentUserOrNull() {
        try {
            return userService.getCurrentUser();
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean canViewPost(ForumPost post, User currentUser) {
        if (post.getStatus() == ForumPostStatus.APPROVED) {
            return true;
        }
        if (currentUser == null) {
            return false;
        }
        boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().name());
        boolean isAuthor = post.getAuthor() != null && post.getAuthor().getId().equals(currentUser.getId());
        return isAdmin || isAuthor;
    }
}
