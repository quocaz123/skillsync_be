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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        List<ForumComment> comments = commentRepository.findAllByPostIdWithAuthorOrderByCreatedAtAsc(postId);
        if (comments.isEmpty()) {
            return List.of();
        }

        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception ignored) {
            // Anonymous readers can still view comments.
        }

        List<UUID> commentIds = comments.stream()
                .map(ForumComment::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<UUID, Long> likeCountByCommentId = new HashMap<>();
        for (CommentVoteRepository.CommentLikeAgg row : commentVoteRepository.aggregateLikesByCommentIds(commentIds)) {
            if (row != null && row.getCommentId() != null) {
                likeCountByCommentId.put(row.getCommentId(), row.getLikeCount() != null ? row.getLikeCount() : 0L);
            }
        }

        Set<UUID> likedByCurrentUser = new HashSet<>();
        if (currentUser != null && currentUser.getId() != null) {
            likedByCurrentUser.addAll(commentVoteRepository.findLikedCommentIds(currentUser.getId(), commentIds));
        }

        Map<UUID, CommentResponse> responseById = new HashMap<>(commentIds.size());
        Map<UUID, List<CommentResponse>> repliesByParentId = new HashMap<>();
        List<CommentResponse> roots = new java.util.ArrayList<>();

        for (ForumComment c : comments) {
            if (c == null || c.getId() == null) continue;
            if (c.getAuthor() == null) {
                throw new IllegalStateException("Comment author is null for comment: " + c.getId());
            }

            UUID id = c.getId();
            UUID parentId = c.getParentComment() != null ? c.getParentComment().getId() : null;
            long likeCount = likeCountByCommentId.getOrDefault(id, 0L);
            boolean liked = currentUser != null && likedByCurrentUser.contains(id);

            CommentResponse resp = CommentResponse.builder()
                    .id(id)
                    .postId(c.getPost().getId())
                    .parentCommentId(parentId)
                    .authorId(c.getAuthor().getId())
                    .authorName(c.getAuthor().getFullName())
                    .authorRole(c.getAuthor().getRole() != null ? c.getAuthor().getRole().name() : "USER")
                    .authorAvatar(c.getAuthor().getAvatarUrl())
                    .content(c.getContent())
                    .likeCount(likeCount)
                    .liked(liked)
                    .replies(List.of())
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt())
                    .build();

            responseById.put(id, resp);

            if (parentId == null) {
                roots.add(resp);
            } else {
                repliesByParentId.computeIfAbsent(parentId, k -> new java.util.ArrayList<>()).add(resp);
            }
        }

        // Attach replies (preserve order because `comments` is ordered by createdAt asc)
        for (Map.Entry<UUID, List<CommentResponse>> e : repliesByParentId.entrySet()) {
            CommentResponse parent = responseById.get(e.getKey());
            if (parent == null) continue;
            List<CommentResponse> children = e.getValue();

            CommentResponse rebuilt = CommentResponse.builder()
                    .id(parent.getId())
                    .postId(parent.getPostId())
                    .parentCommentId(parent.getParentCommentId())
                    .authorId(parent.getAuthorId())
                    .authorName(parent.getAuthorName())
                    .authorRole(parent.getAuthorRole())
                    .authorAvatar(parent.getAuthorAvatar())
                    .content(parent.getContent())
                    .likeCount(parent.getLikeCount())
                    .liked(parent.getLiked())
                    .replies(children)
                    .createdAt(parent.getCreatedAt())
                    .updatedAt(parent.getUpdatedAt())
                    .build();

            responseById.put(parent.getId(), rebuilt);
        }

        // Because we rebuilt some parents, also rebuild roots list to point to latest objects
        return roots.stream()
                .map(r -> responseById.getOrDefault(r.getId(), r))
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

        // Return the updated comment with its nested replies using the optimized tree builder.
        List<CommentResponse> tree = getPostComments(comment.getPost().getId());
        CommentResponse found = findInTree(tree, commentId);
        if (found == null) {
            throw new RuntimeException("Comment not found with id: " + commentId);
        }
        return found;
    }

    /**
     * Get comment count for a post
     */
    public Long getCommentCount(UUID postId) {
        Integer count = commentRepository.countByPostId(postId);
        return count != null ? count.longValue() : 0L;
    }

    public List<ForumCommentRepository.PostCommentAgg> getAggregatedCommentCounts(Collection<UUID> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }
        return commentRepository.aggregateCommentCountsByPostIds(postIds);
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

    private CommentResponse findInTree(List<CommentResponse> roots, UUID commentId) {
        if (roots == null || roots.isEmpty() || commentId == null) {
            return null;
        }
        for (CommentResponse r : roots) {
            if (r == null) continue;
            if (commentId.equals(r.getId())) {
                return r;
            }
            CommentResponse nested = findInTree(r.getReplies(), commentId);
            if (nested != null) {
                return nested;
            }
        }
        return null;
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
