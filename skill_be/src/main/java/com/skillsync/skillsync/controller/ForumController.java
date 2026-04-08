package com.skillsync.skillsync.controller;

import com.skillsync.skillsync.dto.request.forum.CreateCategoryRequest;
import com.skillsync.skillsync.dto.request.forum.CreateCommentRequest;
import com.skillsync.skillsync.dto.request.forum.CreateForumPostRequest;
import com.skillsync.skillsync.dto.request.forum.ToggleVoteRequest;
import com.skillsync.skillsync.dto.request.forum.UpdateCommentRequest;
import com.skillsync.skillsync.dto.request.forum.UpdateForumPostRequest;
import com.skillsync.skillsync.dto.response.forum.CommentResponse;
import com.skillsync.skillsync.dto.response.forum.ForumCategoryResponse;
import com.skillsync.skillsync.dto.response.forum.ForumPostDetailResponse;
import com.skillsync.skillsync.dto.response.forum.ForumPostResponse;
import com.skillsync.skillsync.dto.response.forum.VoteResponse;
import com.skillsync.skillsync.service.ForumCategoryService;
import com.skillsync.skillsync.service.ForumCommentService;
import com.skillsync.skillsync.service.ForumPostService;
import com.skillsync.skillsync.service.ForumRealtimeEventService;
import com.skillsync.skillsync.service.PostSaveService;
import com.skillsync.skillsync.service.PostVoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {
    private final ForumPostService postService;
    private final ForumCommentService commentService;
    private final PostVoteService voteService;
    private final PostSaveService saveService;
    private final ForumCategoryService categoryService;
    private final ForumRealtimeEventService forumRealtimeEventService;

    // ==================== POSTS ENDPOINTS ====================

    /**
     * GET /api/forum - List all posts with pagination, filtering, and searching
     */
    @GetMapping
    public Page<ForumPostResponse> getAllPosts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return postService.getAllPosts(categoryId, search, pageable);
    }

    /**
     * POST /api/forum - Create new post
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public ForumPostResponse createPost(@Valid @RequestBody CreateForumPostRequest request) {
        return postService.createPost(request);
    }

    /**
     * GET /api/forum/trending - Get top trending posts
     */
    @GetMapping("/trending")
    public List<ForumPostResponse> getTrendingPosts(@RequestParam(defaultValue = "10") int limit) {
        return postService.getTrendingPosts(limit);
    }

    /**
     * GET /api/forum/{postId} - Get post detail with all comments
     */
    @GetMapping("/{postId}")
    public ForumPostDetailResponse getPostDetail(@PathVariable UUID postId) {
        return postService.getPostById(postId);
    }

    /**
     * PUT /api/forum/{postId} - Update post (only author)
     */
    @PutMapping("/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ForumPostResponse updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdateForumPostRequest request) {
        return postService.updatePost(postId, request);
    }

    /**
     * DELETE /api/forum/{postId} - Delete post (only author)
     */
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void deletePost(@PathVariable UUID postId) {
        postService.deletePost(postId);
    }

    /**
     * GET /api/forum/user/{userId} - Get user's posts
     */
    @GetMapping("/user/{userId}")
    public Page<ForumPostResponse> getUserPosts(
            @PathVariable UUID userId,
            Pageable pageable) {
        return postService.getUserPosts(userId, pageable);
    }

    // ==================== CATEGORIES ENDPOINTS ====================

    /**
     * GET /api/forum/categories - List all categories
     */
    @GetMapping("/categories")
    public List<ForumCategoryResponse> getAllCategories() {
        return categoryService.getAllCategories();
    }

    /**
     * GET /api/forum/events - Real-time forum moderation updates
     */
    @GetMapping(value = "/events", produces = "text/event-stream")
    @PreAuthorize("isAuthenticated()")
    public SseEmitter subscribeForumEvents() {
        return forumRealtimeEventService.subscribe();
    }

    /**
     * POST /api/forum/categories - Create category (admin only)
     */
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ForumCategoryResponse createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(request);
    }

    // ==================== COMMENTS ENDPOINTS ====================

    /**
     * POST /api/forum/{postId}/comments - Add comment to post
     */
    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public CommentResponse addComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {
        return commentService.addComment(postId, request);
    }

    /**
     * PUT /api/forum/comments/{commentId} - Update comment (only author)
     */
    @PutMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public CommentResponse updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return commentService.updateComment(commentId, request);
    }

    /**
     * DELETE /api/forum/comments/{commentId} - Delete comment (only author)
     */
    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void deleteComment(@PathVariable UUID commentId) {
        commentService.deleteComment(commentId);
    }

    /**
     * POST /api/forum/comments/{commentId}/vote - Toggle like on comment
     */
    @PostMapping("/comments/{commentId}/vote")
    @PreAuthorize("isAuthenticated()")
    public CommentResponse toggleCommentLike(@PathVariable UUID commentId) {
        return commentService.toggleLike(commentId);
    }

    // ==================== VOTES ENDPOINTS ====================

    /**
     * POST /api/forum/{postId}/vote - Toggle vote on post (upvote/downvote)
     */
    @PostMapping("/{postId}/vote")
    @PreAuthorize("isAuthenticated()")
    public VoteResponse toggleVote(
            @PathVariable UUID postId,
            @Valid @RequestBody ToggleVoteRequest request) {
        return voteService.toggleVote(postId, request.getVoteType());
    }

    /**
     * GET /api/forum/{postId}/votes - Get vote counts for a post
     */
    @GetMapping("/{postId}/votes")
    public PostVoteCountDto getVoteCounts(@PathVariable UUID postId) {
        PostVoteService.VoteCountResponse counts = voteService.getVoteCounts(postId);
        return new PostVoteCountDto(counts.upvotes, counts.downvotes);
    }

    // ==================== SAVES ENDPOINTS ====================

    /**
     * POST /api/forum/{postId}/save - Save/unsave post (toggle)
     */
    @PostMapping("/{postId}/save")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()")
    public void toggleSavePost(@PathVariable UUID postId) {
        saveService.toggleSave(postId);
    }

    /**
     * GET /api/forum/saved - Get current user's saved posts
     */
    @GetMapping("/saved")
    @PreAuthorize("isAuthenticated()")
    public Page<ForumPostResponse> getSavedPosts(Pageable pageable) {
        return saveService.getUserSavedPosts(pageable);
    }

    // ==================== INNER CLASS FOR RESPONSE ====================

    /**
     * DTO for vote counts response
     */
    public static class PostVoteCountDto {
        public Long upvotes;
        public Long downvotes;

        public PostVoteCountDto(Long upvotes, Long downvotes) {
            this.upvotes = upvotes;
            this.downvotes = downvotes;
        }
    }
}
