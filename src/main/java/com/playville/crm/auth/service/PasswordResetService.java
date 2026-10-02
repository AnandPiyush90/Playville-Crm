package com.playville.crm.auth.service;

import com.playville.crm.auth.entity.PasswordResetToken;
import com.playville.crm.common.ApiResponse;
import com.playville.crm.entity.Staff;
import com.playville.crm.repository.PasswordResetTokenRepository;
import com.playville.crm.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_TTL_MINUTES = 15;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailService passwordResetMailService;
    private final PasswordResetRateLimiter passwordResetRateLimiter;
    private final PasswordResetAuditService passwordResetAuditService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ApiResponse<Void> requestPasswordReset(String email) {
        return requestPasswordReset(email, null);
    }

    @Transactional
    public ApiResponse<Void> requestPasswordReset(String email, String ipAddress) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return ApiResponse.success("If an account exists, password reset instructions have been sent.");
        }

        if (passwordResetRateLimiter.isThrottled(normalizedEmail, ipAddress)) {
            passwordResetAuditService.logThrottled(normalizedEmail, ipAddress);
            return ApiResponse.success("If an account exists, password reset instructions have been sent.");
        }

        Optional<Staff> staffOpt = staffRepository.findByEmailIgnoreCase(normalizedEmail);
        if (staffOpt.isEmpty()) {
            passwordResetAuditService.logPasswordResetRequest(normalizedEmail, ipAddress, "not_found");
            return ApiResponse.success("If an account exists, password reset instructions have been sent.");
        }

        Staff staff = staffOpt.get();
        String accountEmail = staff.getEmail() == null ? normalizedEmail : staff.getEmail().trim().toLowerCase(Locale.ROOT);

        passwordResetTokenRepository.deleteByEmail(accountEmail);
        String rawToken = generateToken();
        String tokenHash = passwordEncoder.encode(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setEmail(accountEmail);
        resetToken.setTokenHash(tokenHash);
        resetToken.setCreatedAt(LocalDateTime.now());
        resetToken.setExpiresAt(resetToken.getCreatedAt().plusMinutes(TOKEN_TTL_MINUTES));
        passwordResetTokenRepository.save(resetToken);

        Integer branchId = staff.getBranch() != null ? staff.getBranch().getId() : null;
        passwordResetMailService.sendPasswordResetEmail(branchId, accountEmail, rawToken);
        passwordResetAuditService.logPasswordResetRequest(accountEmail, ipAddress, "sent");

        return ApiResponse.success("If an account exists, password reset instructions have been sent.");
    }

    @Transactional
    public ApiResponse<Void> resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            passwordResetAuditService.logInvalidTokenAttempt("missing-token");
            return ApiResponse.failure("Reset token is required.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            passwordResetAuditService.logInvalidTokenAttempt(maskToken(token));
            return ApiResponse.failure("Password must be at least 8 characters long.");
        }

        List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findActiveTokens(LocalDateTime.now());
        PasswordResetToken matchedToken = null;

        for (PasswordResetToken candidate : activeTokens) {
            if (passwordEncoder.matches(token, candidate.getTokenHash())) {
                matchedToken = candidate;
                break;
            }
        }

        if (matchedToken == null) {
            passwordResetAuditService.logInvalidTokenAttempt(maskToken(token));
            return ApiResponse.failure("Invalid or expired reset token.");
        }

        if (matchedToken.getUsedAt() != null || matchedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetAuditService.logExpiredTokenAttempt(maskToken(token), matchedToken.getEmail());
            return ApiResponse.failure("Invalid or expired reset token.");
        }

        Optional<Staff> staffOpt = staffRepository.findByEmailIgnoreCase(matchedToken.getEmail());
        if (staffOpt.isEmpty()) {
            passwordResetAuditService.logInvalidTokenAttempt(maskToken(token));
            return ApiResponse.failure("Invalid or expired reset token.");
        }

        Staff staff = staffOpt.get();
        staff.setPasswordHash(passwordEncoder.encode(newPassword));
        staff.setTokenVersion((staff.getTokenVersion() == null ? 0 : staff.getTokenVersion()) + 1);
        staffRepository.save(staff);

        matchedToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(matchedToken);
        passwordResetTokenRepository.deleteByEmail(matchedToken.getEmail());

        passwordResetAuditService.logPasswordResetSuccess(staff.getId(), matchedToken.getEmail());

        return ApiResponse.success("Password updated successfully.");
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
