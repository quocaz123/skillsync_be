package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.ForumComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumComment, UUID> {
    List<ForumComment> findByPostIdAndParentCommentIsNullOrderByCreatedAtAsc(UUID postId);

    List<ForumComment> findByParentCommentIdOrderByCreatedAtAsc(UUID parentCommentId);

    List<ForumComment> findByPostIdOrderByCreatedAtAsc(UUID postId);

    @EntityGraph(attributePaths = {"author"})
    @Query("""
            select c from ForumComment c
            where c.post.id = :postId
            order by c.createdAt asc
            """)
    List<ForumComment> findAllByPostIdWithAuthorOrderByCreatedAtAsc(@Param("postId") UUID postId);

    List<ForumComment> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    Integer countByPostId(UUID postId);

    interface PostCommentAgg {
        UUID getPostId();

        Long getCommentCount();
    }

    @Query("""
            select c.post.id as postId,
                   count(c.id) as commentCount
            from ForumComment c
            where c.post.id in :postIds
            group by c.post.id
            """)
    List<PostCommentAgg> aggregateCommentCountsByPostIds(@Param("postIds") Collection<UUID> postIds);
}
