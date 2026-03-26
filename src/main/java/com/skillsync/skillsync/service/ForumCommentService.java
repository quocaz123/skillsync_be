package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.request.forum.CreateCommentRequest;
import com.skillsync.skillsync.dto.request.forum.UpdateCommentRequest;
import com.skillsync.skillsync.dto.response.forum.CommentResponse;
import com.skillsync.skillsync.entity.ForumComment;
import com.skillsync.skillsync.entity.ForumPost;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.repository.ForumCommentRepository;
import com.skillsync.skillsync.repository.ForumPostRepository;
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
        List<CommentResponse> replies = getCommentReplies(comment.getId());

        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getFullName())
                .authorRole(comment.getAuthor().getRole() != null ? comment.getAuthor().getRole().name() : "USER")
                .authorAvatar(comment.getAuthor().getAvatarUrl())
                .content(comment.getContent())
                .replies(replies)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /**
     * Convert entity to response (without nested replies)
     */
    private CommentResponse toResponse(ForumComment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getFullName())
                .authorRole(comment.getAuthor().getRole() != null ? comment.getAuthor().getRole().name() : "USER")
                .authorAvatar(comment.getAuthor().getAvatarUrl())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
