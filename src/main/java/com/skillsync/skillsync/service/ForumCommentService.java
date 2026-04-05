package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.forum.CreateCommentRequest;
import com.skillsync.skillsync.dto.request.forum.UpdateCommentRequest;
import com.skillsync.skillsync.dto.response.forum.CommentResponse;
import com.skillsync.skillsync.entity.ForumComment;
import com.skillsync.skillsync.entity.ForumPost;
import com.skillsync.skillsync.entity.CommentVote;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.ForumPostStatus;
import com.skillsync.skillsync.repository.ForumCommentRepository;
import com.skillsync.skillsync.repository.ForumPostRepository;
import com.skillsync.skillsync.repository.CommentVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForumCommentService {
    private final ForumCommentRepository commentRepository;
    private final ForumPostRepository postRepository;
    private final CommentVoteRepository commentVoteRepository;
    private final UserService userService;

    /**
     * Get all comments for a post (nested structure)
     */
    public List<CommentResponse> getPostComments(UUID postId) {
        List<ForumComment> rootComments = commentRepository.findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(postId);
        return rootComments.stream()
                .map(this::toResponseWithReplies)
                .toList();
    }

    /**
     * Toggle like for a comment
     */
    @Transactional
    public CommentResponse toggleLike(UUID commentId) {
        User currentUser = userService.getCurrentUser();

        ForumComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        ensurePostAccessible(comment.getPost(), currentUser);

        if (commentVoteRepository.existsByCommentIdAndUserId(commentId, currentUser.getId())) {
            commentVoteRepository.deleteByCommentIdAndUserId(commentId, currentUser.getId());
        } else {
            CommentVote vote = CommentVote.builder()
                    .comment(comment)
                    .user(currentUser)
                    .build();
            commentVoteRepository.save(vote);
        }

        return toResponseWithReplies(comment);
    }

    /**
     * Get comment count for a post
     */
    public Long getCommentCount(UUID postId) {
        Integer count = commentRepository.countByPostId(postId);
        return count != null ? count.longValue() : 0L;
    }

    /**
     * Add comment to post
     */
    @Transactional
    public CommentResponse addComment(UUID postId, CreateCommentRequest request) {
        User author = userService.getCurrentUser();

        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        ensurePostAccessible(post, author);

        ForumComment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found with id: " + request.getParentCommentId()));
        }

        ForumComment comment = ForumComment.builder()
                .post(post)
                .author(author)
                .parentComment(parentComment)
                .content(request.getContent())
                .build();

        ForumComment saved = commentRepository.save(comment);
        return toResponse(saved);
    }

    /**
     * Update comment (only author)
     */
    @Transactional
    public CommentResponse updateComment(UUID commentId, UpdateCommentRequest request) {
        User currentUser = userService.getCurrentUser();

        ForumComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized: only author can update this comment");
        }

        ensurePostAccessible(comment.getPost(), currentUser);

        if (request.getContent() != null) {
            comment.setContent(request.getContent());
        }

        ForumComment updated = commentRepository.save(comment);
        return toResponse(updated);
    }

    /**
     * Delete comment (only author)
     */
    @Transactional
    public void deleteComment(UUID commentId) {
        User currentUser = userService.getCurrentUser();

        ForumComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized: only author can delete this comment");
        }

        ensurePostAccessible(comment.getPost(), currentUser);

        commentRepository.delete(comment);
    }

    /**
     * Get comment replies (nested children)
     */
    public List<CommentResponse> getCommentReplies(UUID parentCommentId) {
        List<ForumComment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(parentCommentId);
        return replies.stream()
                .map(this::toResponseWithReplies)
                .toList();
    }

    /**
     * Convert entity to response with nested replies
     */
    private CommentResponse toResponseWithReplies(ForumComment comment) {
        if (comment.getAuthor() == null) {
            throw new IllegalStateException("Comment author is null for comment: " + comment.getId());
        }

        User author = comment.getAuthor();
        List<CommentResponse> replies = getCommentReplies(comment.getId());
        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception ignored) {
            // Anonymous readers can still view comments.
        }

        Long likeCount = commentVoteRepository.countByCommentId(comment.getId());
        Boolean liked = currentUser != null && commentVoteRepository.existsByCommentIdAndUserId(comment.getId(), currentUser.getId());

        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .authorId(author.getId())
                .authorName(author.getFullName())
                .authorRole(author.getRole() != null ? author.getRole().name() : "USER")
                .authorAvatar(author.getAvatarUrl())
                .content(comment.getContent())
                .likeCount(likeCount)
                .liked(liked)
                .replies(replies)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /**
     * Convert entity to response (without nested replies)
     */
    private CommentResponse toResponse(ForumComment comment) {
        if (comment.getAuthor() == null) {
            throw new IllegalStateException("Comment author is null for comment: " + comment.getId());
        }

        User author = comment.getAuthor();
        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception ignored) {
            // Anonymous readers can still view comments.
        }

        Long likeCount = commentVoteRepository.countByCommentId(comment.getId());
        Boolean liked = currentUser != null && commentVoteRepository.existsByCommentIdAndUserId(comment.getId(), currentUser.getId());

        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .authorId(author.getId())
                .authorName(author.getFullName())
                .authorRole(author.getRole() != null ? author.getRole().name() : "USER")
                .authorAvatar(author.getAvatarUrl())
                .content(comment.getContent())
                .likeCount(likeCount)
                .liked(liked)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    private void ensurePostAccessible(ForumPost post, User currentUser) {
        if (post == null) {
            throw new RuntimeException("Post not found");
        }

        boolean isAdmin = currentUser != null && currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().name());
        boolean isAuthor = currentUser != null && post.getAuthor() != null && post.getAuthor().getId().equals(currentUser.getId());
        boolean approved = post.getStatus() == ForumPostStatus.APPROVED;

        if (!approved && !isAdmin && !isAuthor) {
            throw new RuntimeException("Post not found");
        }
    }
}
