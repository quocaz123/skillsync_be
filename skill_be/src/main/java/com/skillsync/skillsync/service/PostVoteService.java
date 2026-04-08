package com.skillsync.skillsync.service;

import com.skillsync.skillsync.dto.response.forum.VoteResponse;
import com.skillsync.skillsync.entity.ForumPost;
import com.skillsync.skillsync.entity.PostVote;
import com.skillsync.skillsync.entity.User;
import com.skillsync.skillsync.enums.ForumPostStatus;
import com.skillsync.skillsync.enums.VoteType;
import com.skillsync.skillsync.repository.ForumPostRepository;
import com.skillsync.skillsync.repository.PostVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostVoteService {
    private final PostVoteRepository voteRepository;
    private final ForumPostRepository postRepository;
    private final UserService userService;

    /**
     * Toggle vote on a post (upvote/downvote/no vote)
     * Logic:
     * - If no vote: insert the requested vote
     * - If same vote: remove the vote
     * - If different vote: update to the new vote type
     */
    @Transactional
    public VoteResponse toggleVote(UUID postId, VoteType voteType) {
        User user = userService.getCurrentUser();

        ForumPost post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        ensurePostAccessible(post, user);

        Optional<PostVote> existingVote = voteRepository.findByPostIdAndUserId(postId, user.getId());

        if (existingVote.isPresent()) {
            PostVote vote = existingVote.get();
            if (vote.getVoteType() == voteType) {
                // Same vote type: remove it
                voteRepository.delete(vote);
                return null; // No vote
            } else {
                // Different vote type: update it
                vote.setVoteType(voteType);
                PostVote updated = voteRepository.save(vote);
                return toResponse(updated);
            }
        } else {
            // No existing vote: create new one
            PostVote newVote = PostVote.builder()
                    .post(post)
                    .user(user)
                    .voteType(voteType)
                    .build();
            PostVote saved = voteRepository.save(newVote);
            return toResponse(saved);
        }
    }

    /**
     * Get vote counts for a post
     */
    public VoteCountResponse getVoteCounts(UUID postId) {
        Long upvotes = voteRepository.countByPostIdAndVoteType(postId, VoteType.UPVOTE);
        Long downvotes = voteRepository.countByPostIdAndVoteType(postId, VoteType.DOWNVOTE);

        return VoteCountResponse.builder()
                .upvotes(upvotes)
                .downvotes(downvotes)
                .build();
    }

    /**
     * Get user's vote on a post (null if no vote)
     */
    public VoteResponse getUserVote(UUID postId, UUID userId) {
        return voteRepository.findByPostIdAndUserId(postId, userId)
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * Convert entity to response
     */
    private VoteResponse toResponse(PostVote vote) {
        return VoteResponse.builder()
                .id(vote.getId())
                .postId(vote.getPost().getId())
                .userId(vote.getUser().getId())
                .voteType(vote.getVoteType())
                .createdAt(vote.getCreatedAt())
                .build();
    }

    private void ensurePostAccessible(ForumPost post, User currentUser) {
        boolean isAdmin = currentUser != null && currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().name());
        boolean isAuthor = currentUser != null && post.getAuthor() != null && post.getAuthor().getId().equals(currentUser.getId());
        if (post.getStatus() != ForumPostStatus.APPROVED && !isAdmin && !isAuthor) {
            throw new RuntimeException("Post not found with id: " + post.getId());
        }
    }

    /**
     * Inner class for vote count response
     */
    public static class VoteCountResponse {
        public Long upvotes;
        public Long downvotes;

        public VoteCountResponse(Long upvotes, Long downvotes) {
            this.upvotes = upvotes;
            this.downvotes = downvotes;
        }

        public static VoteCountResponseBuilder builder() {
            return new VoteCountResponseBuilder();
        }

        public static class VoteCountResponseBuilder {
            private Long upvotes;
            private Long downvotes;

            public VoteCountResponseBuilder upvotes(Long upvotes) {
                this.upvotes = upvotes;
                return this;
            }

            public VoteCountResponseBuilder downvotes(Long downvotes) {
                this.downvotes = downvotes;
                return this;
            }

            public VoteCountResponse build() {
                return new VoteCountResponse(upvotes, downvotes);
            }
        }
    }
}
