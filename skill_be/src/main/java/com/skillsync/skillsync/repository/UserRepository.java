package com.skillsync.skillsync.repository;

import com.skillsync.skillsync.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    long countByRole(com.skillsync.skillsync.enums.Role role);
    long countByStatus(com.skillsync.skillsync.enums.UserStatus status);

    /** Tìm user theo tên hoặc email — dùng cho @mention dropdown */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY u.fullName ASC")
    List<User> searchForMention(@Param("q") String q, Pageable pageable);

    /**
     * Lấy tất cả user (dành cho admin) — không lọc search.
     * Tách khỏi query có LOWER() để tránh lỗi PostgreSQL lower(bytea).
     */
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    org.springframework.data.domain.Page<User> findAllForAdmin(Pageable pageable);

    /**
     * Tìm kiếm user theo tên hoặc email (dành cho admin).
     * Dùng native SQL với cast ::text rõ ràng để tránh lỗi lower(bytea).
     */
    @Query(value = "SELECT * FROM users u WHERE " +
                   "lower(u.full_name::text) LIKE lower(('%' || :search || '%')) OR " +
                   "lower(u.email::text) LIKE lower(('%' || :search || '%')) " +
                   "ORDER BY u.created_at DESC",
           countQuery = "SELECT count(*) FROM users u WHERE " +
                        "lower(u.full_name::text) LIKE lower(('%' || :search || '%')) OR " +
                        "lower(u.email::text) LIKE lower(('%' || :search || '%'))",
           nativeQuery = true)
    org.springframework.data.domain.Page<User> searchAllForAdmin(@Param("search") String search, Pageable pageable);
}

