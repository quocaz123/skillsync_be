package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.forum.CreateForumPostRequest;
import com.skillsync.skillsync.dto.request.forum.UpdateForumPostRequest;
import com.skillsync.skillsync.dto.response.forum.ForumPostDetailResponse;
import com.skillsync.skillsync.dto.response.forum.ForumPostResponse;
import com.skillsync.skillsync.entity.ForumCategory;
import com.skillsync.skillsync.entity.ForumPost;
import com.skillsync.skillsync.entity.PostSave;
import com.skillsync.skillsync.entity.PostVote;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.VoteType;
import com.skillsync.skillsync.repository.ForumCategoryRepository;
import com.skillsync.skillsync.repository.ForumPostRepository;
import com.skillsync.skillsync.repository.PostSaveRepository;
import com.skillsync.skillsync.repository.PostVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * Get all posts with pagination
     */
    public Page<ForumPostResponse> getAllPosts(UUID categoryId, String searchKeyword, Pageable pageable) {
        Page<ForumPost> posts;

        if (categoryId != null && searchKeyword != null) {
            posts = postRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId, pageable);
        } else if (categoryId != null) {
            posts = postRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId, pageable);
        } else if (searchKeyword != null && !searchKeyword.isEmpty()) {
            posts = postRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByCreatedAtDesc(
                    searchKeyword, searchKeyword, pageable);
        } else {
            posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return posts.map(this::toResponse);
    }

    /**
     * Get post by ID with comments
     */
    public ForumPostDetailResponse getPostById(UUID postId) {
        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        return toDetailResponse(post);
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
                .postType(request.getPostType())
                .tags(tagsString)
                .solved(false)
                .build();

        ForumPost saved = postRepository.save(post);
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
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }
        if (request.getPostType() != null) {
            post.setPostType(request.getPostType());
        }
        if (request.getTags() != null) {
            post.setTags(String.join(",", request.getTags()));
        }
        if (request.getSolved() != null) {
            post.setSolved(request.getSolved());
        }

        ForumPost updated = postRepository.save(post);
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

        postRepository.delete(post);
    }

    /**
     * Get trending posts (top 10 by recent + vote count)
     */
    public List<ForumPostResponse> getTrendingPosts(int limit) {
        List<ForumPost> posts = postRepository.findTop10ByOrderByCreatedAtDesc();
        return posts.stream()
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get user's posts
     */
    public Page<ForumPostResponse> getUserPosts(UUID userId, Pageable pageable) {
        Page<ForumPost> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable);
        return posts.map(this::toResponse);
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

        List<String> tags = post.getTags() != null && !post.getTags().isEmpty()
                ? List.of(post.getTags().split(","))
                : List.of();

        return ForumPostResponse.builder()
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
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    /**
     * Convert entity to detail response with comments
     */
    private ForumPostDetailResponse toDetailResponse(ForumPost post) {
        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception e) {
            // Ignore if not authenticated
        }

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
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
