package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.response.forum.ForumPostResponse;
import com.skillsync.skillsync.entity.ForumCategory;
import com.skillsync.skillsync.entity.ForumPost;
import com.skillsync.skillsync.entity.PostSave;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.VoteType;
import com.skillsync.skillsync.repository.ForumPostRepository;
import com.skillsync.skillsync.repository.PostSaveRepository;
import com.skillsync.skillsync.repository.PostVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        User user = userService.getCurrentUser();

        Page<PostSave> saves = saveRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        return saves.map(save -> toPostResponse(save.getPost(), user));
    }

    /**
     * Check if post is saved by current user
     */
    public boolean isPostSaved(UUID postId) {
        User user = userService.getCurrentUser();
        return saveRepository.existsByPostIdAndUserId(postId, user.getId());
    }

    /**
     * Convert ForumPost entity to response DTO
     */
    private ForumPostResponse toPostResponse(ForumPost post, User currentUser) {
        // Validate required relationships
        if (post.getAuthor() == null) {
            throw new IllegalStateException("Post author is null for post: " + post.getId());
        }
        if (post.getCategory() == null) {
            throw new IllegalStateException("Post category is null for post: " + post.getId());
        }

        User author = post.getAuthor();
        ForumCategory category = post.getCategory();

        Long upvotes = voteRepository.countByPostIdAndVoteType(post.getId(), VoteType.UPVOTE);
        Long downvotes = voteRepository.countByPostIdAndVoteType(post.getId(), VoteType.DOWNVOTE);
        Long commentCount = commentService.getCommentCount(post.getId());
        Long saveCount = saveRepository.countByPostId(post.getId());

        Boolean liked = voteRepository.findByPostIdAndUserId(post.getId(), currentUser.getId())
                .map(v -> v.getVoteType() == VoteType.UPVOTE)
                .orElse(false);
        Boolean saved = true; // Since we're getting from saved posts

        java.util.List<String> tags = post.getTags() != null && !post.getTags().isEmpty()
                ? java.util.List.of(post.getTags().split(","))
                : java.util.List.of();

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
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
