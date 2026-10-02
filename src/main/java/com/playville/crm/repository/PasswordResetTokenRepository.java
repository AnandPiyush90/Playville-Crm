package com.playville.crm.repository;

import com.playville.crm.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByEmailOrderByCreatedAtDesc(String email);

    @Query("select p from PasswordResetToken p where p.email = :email and p.usedAt is null and p.expiresAt > :now order by p.createdAt desc")
    List<PasswordResetToken> findActiveByEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    @Query("select p from PasswordResetToken p where p.usedAt is null and p.expiresAt > :now order by p.createdAt desc")
    List<PasswordResetToken> findActiveTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from PasswordResetToken p where p.email = :email")
    void deleteByEmail(@Param("email") String email);
}
