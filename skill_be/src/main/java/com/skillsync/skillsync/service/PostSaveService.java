package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.response.forum.ForumPostResponse;
import com.skillsync.skillsync.entity.ForumCategory;
import com.skillsync.skillsync.entity.ForumPost;
import com.skillsync.skillsync.entity.PostSave;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.ForumPostStatus;
import com.skillsync.skillsync.repository.ForumPostRepository;
import com.skillsync.skillsync.repository.PostSaveRepository;
import com.skillsync.skillsync.repository.PostVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostSaveService {
    private final PostSaveRepository saveRepository;
    private final ForumPostRepository postRepository;
    private final PostVoteRepository voteRepository;
    private final ForumCommentService commentService;
    private final UserService userService;

    /**
     * Save a post for current user
     */
    @Transactional
    public void savePost(UUID postId) {
        User user = userService.getCurrentUser();

        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        ensurePostAccessible(post, user);

        if (!saveRepository.existsByPostIdAndUserId(postId, user.getId())) {
            PostSave save = PostSave.builder()
                    .post(post)
                    .user(user)
                    .build();
            saveRepository.save(save);
        }
    }

    /**
     * Unsave a post for current user
     */
    @Transactional
    public void unsavePost(UUID postId) {
        User user = userService.getCurrentUser();

        if (saveRepository.existsByPostIdAndUserId(postId, user.getId())) {
            saveRepository.deleteByPostIdAndUserId(postId, user.getId());
        }
    }

    /**
     * Toggle save state for a post
     */
    @Transactional
    public void toggleSave(UUID postId) {
        User user = userService.getCurrentUser();

        if (saveRepository.existsByPostIdAndUserId(postId, user.getId())) {
            saveRepository.deleteByPostIdAndUserId(postId, user.getId());
        } else {
            ForumPost post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

            ensurePostAccessible(post, user);

            PostSave save = PostSave.builder()
                    .post(post)
                    .user(user)
                    .build();
            saveRepository.save(save);
        }
    }

    /**
     * Get current user's saved posts
     */
    public Page<ForumPostResponse> getUserSavedPosts(Pageable pageable) {
        User user = getCurrentUserOrNull();
        if (user == null) {
            return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0);
        }

        Page<PostSave> saves = saveRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        List<ForumPost> posts = saves.stream()
                .map(PostSave::getPost)
                .filter(Objects::nonNull)
                .filter(post -> canAccess(post, user))
                .toList();

        List<UUID> postIds = posts.stream()
                .map(ForumPost::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<UUID, Long> upvotesByPostId = new HashMap<>();
        Map<UUID, Long> downvotesByPostId = new HashMap<>();
        for (PostVoteRepository.PostVoteAgg row : voteRepository.aggregateVotesByPostIds(postIds)) {
            if (row != null && row.getPostId() != null) {
                upvotesByPostId.put(row.getPostId(), row.getUpvotes() != null ? row.getUpvotes() : 0L);
                downvotesByPostId.put(row.getPostId(), row.getDownvotes() != null ? row.getDownvotes() : 0L);
            }
        }

        Map<UUID, Long> commentCountByPostId = new HashMap<>();
        for (com.skillsync.skillsync.repository.ForumCommentRepository.PostCommentAgg row : commentService.getAggregatedCommentCounts(postIds)) {
            if (row != null && row.getPostId() != null) {
                commentCountByPostId.put(row.getPostId(), row.getCommentCount() != null ? row.getCommentCount() : 0L);
            }
        }

        Map<UUID, Long> saveCountByPostId = new HashMap<>();
        for (PostSaveRepository.PostSaveAgg row : saveRepository.aggregateSavesByPostIds(postIds)) {
            if (row != null && row.getPostId() != null) {
                saveCountByPostId.put(row.getPostId(), row.getSaveCount() != null ? row.getSaveCount() : 0L);
            }
        }

        Set<UUID> upvotedByCurrentUser = Set.copyOf(voteRepository.findUpvotedPostIds(user.getId(), postIds));

        List<ForumPostResponse> content = posts.stream()
                .map(post -> {
                    try {
                        if (post.getAuthor() == null) {
                            throw new IllegalStateException("Post author is null for post: " + post.getId());
                        }
                        if (post.getCategory() == null) {
                            throw new IllegalStateException("Post category is null for post: " + post.getId());
                        }

                        User author = post.getAuthor();
                        ForumCategory category = post.getCategory();
                        UUID postId = post.getId();

                        boolean liked = postId != null && upvotedByCurrentUser.contains(postId);
                        boolean saved = true;

                        java.util.List<String> tags = post.getTags() != null && !post.getTags().isEmpty()
                                ? java.util.List.of(post.getTags().split(","))
                                : java.util.List.of();

                        return ForumPostResponse.builder()
                                .id(postId)
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
                                .upvotes(upvotesByPostId.getOrDefault(postId, 0L))
                                .downvotes(downvotesByPostId.getOrDefault(postId, 0L))
                                .commentCount(commentCountByPostId.getOrDefault(postId, 0L))
                                .saveCount(saveCountByPostId.getOrDefault(postId, 0L))
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
                    } catch (Exception ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        return new PageImpl<>(content, pageable, saves.getTotalElements());
    }

    /**
     * Check if post is saved by current user
     */
    public boolean isPostSaved(UUID postId) {
        User user = getCurrentUserOrNull();
        if (user == null) {
            return false;
        }
        return saveRepository.existsByPostIdAndUserId(postId, user.getId());
    }

    /**
     * Convert ForumPost entity to response DTO
     */
    private void ensurePostAccessible(ForumPost post, User currentUser) {
        if (!canAccess(post, currentUser)) {
            throw new RuntimeException("Post not found with id: " + post.getId());
        }
    }

    private boolean canAccess(ForumPost post, User currentUser) {
        if (post == null || currentUser == null) {
            return false;
        }
        boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().name());
        boolean isAuthor = post.getAuthor() != null && post.getAuthor().getId().equals(currentUser.getId());
        return post.getStatus() == ForumPostStatus.APPROVED || isAdmin || isAuthor;
    }

    private User getCurrentUserOrNull() {
        try {
            return userService.getCurrentUser();
        } catch (Exception ignored) {
            return null;
        }
    }
}
