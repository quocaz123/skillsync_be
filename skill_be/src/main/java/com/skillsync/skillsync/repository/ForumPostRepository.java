package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.ForumPost;
import com.skillsync.skillsync.enums.ForumPostStatus;
import com.skillsync.skillsync.enums.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, UUID> {
    @EntityGraph(attributePaths = {"author", "category", "reviewedBy"})
    Page<ForumPost> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "reviewedBy"})
    Page<ForumPost> findByAuthorIdAndStatusOrderByCreatedAtDesc(UUID authorId, ForumPostStatus status, Pageable pageable);

    Page<ForumPost> findByCategoryIdOrderByCreatedAtDesc(UUID categoryId, Pageable pageable);

    Page<ForumPost> findByPostTypeOrderByCreatedAtDesc(PostType postType, Pageable pageable);

    Page<ForumPost> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByCreatedAtDesc(
            String title, String content, Pageable pageable);

    List<ForumPost> findTop10ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"author", "category", "reviewedBy"})
    Page<ForumPost> findByStatusOrderByCreatedAtDesc(ForumPostStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "reviewedBy"})
    List<ForumPost> findByStatusOrderByCreatedAtDesc(ForumPostStatus status);

    Page<ForumPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "reviewedBy"})
    @Query("""
            select p from ForumPost p
                                                where p.status = :status
                                                        and (:categoryId is null or p.category.id = :categoryId)
              and (
                    :search is null or :search = ''
                    or lower(p.title) like lower(concat('%', :search, '%'))
                    or lower(p.content) like lower(concat('%', :search, '%'))
                  )
            order by p.createdAt desc
            """)
    Page<ForumPost> searchByStatusAndCategoryAndKeyword(
                                                @Param("status") ForumPostStatus status,
                                                @Param("categoryId") UUID categoryId,
                                                @Param("search") String search,
            Pageable pageable);

    @Query("""
            select p from ForumPost p
            where p.status = :status
              and (:authorId is null or p.author.id = :authorId)
            order by p.createdAt desc
            """)
    Page<ForumPost> findByStatusAndAuthorIdOrderByCreatedAtDesc(
            @Param("status") ForumPostStatus status,
            @Param("authorId") UUID authorId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"author", "category", "reviewedBy"})
    @Query("select p from ForumPost p where p.id = :id")
    Optional<ForumPost> findWithDetailsById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"author", "category", "reviewedBy"})
    @Query("""
            select p
            from ForumPost p
            where p.status = :status
            order by
                                                        (select count(v.id) from PostVote v where v.post.id = p.id and v.voteType = com.skillsync.skillsync.enums.VoteType.UPVOTE) desc,
              (
                                                                (select count(c.id) from ForumComment c where c.post.id = p.id)
                + (select count(s.id) from PostSave s where s.post.id = p.id)
              ) desc,
              p.createdAt desc
            """)
                Page<ForumPost> findTrendingByStatus(@Param("status") ForumPostStatus status, Pageable pageable);
}
