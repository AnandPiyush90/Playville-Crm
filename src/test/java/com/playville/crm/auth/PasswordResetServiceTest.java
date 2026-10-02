package com.playville.crm.auth;

import com.playville.crm.auth.service.PasswordResetMailService;
import com.playville.crm.auth.service.PasswordResetAuditService;
import com.playville.crm.auth.service.PasswordResetRateLimiter;
import com.playville.crm.auth.service.PasswordResetService;
import com.playville.crm.entity.Branch;
import com.playville.crm.entity.Staff;
import com.playville.crm.repository.PasswordResetTokenRepository;
import com.playville.crm.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordResetMailService passwordResetMailService;

    @Mock
    private PasswordResetRateLimiter passwordResetRateLimiter;

    @Mock
    private PasswordResetAuditService passwordResetAuditService;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                passwordResetTokenRepository,
                staffRepository,
                new BCryptPasswordEncoder(),
                passwordResetMailService,
                passwordResetRateLimiter,
                passwordResetAuditService
        );
    }

    @Test
    void requestPasswordReset_shouldCreateTokenAndSendEmailUsingCurrentBranchConfig() {
        Staff staff = new Staff();
        Branch branch = new Branch();
        branch.setId(7);
        staff.setBranch(branch);
        staff.setEmail("staff@example.com");
        when(staffRepository.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(staff));

        var result = passwordResetService.requestPasswordReset("staff@example.com");

        assertTrue(result.isSuccess());
        verify(passwordResetTokenRepository).deleteByEmail("staff@example.com");
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetMailService).sendPasswordResetEmail(eq(7), eq("staff@example.com"), tokenCaptor.capture());
        assertNotNull(tokenCaptor.getValue());
    }

    @Test
    void resetPassword_shouldHashNewPasswordAndInvalidateToken() {
        Staff staff = new Staff();
        staff.setEmail("staff@example.com");
        staff.setPasswordHash("oldhash");
        when(staffRepository.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(staff));

        var token = new com.playville.crm.auth.entity.PasswordResetToken();
        token.setEmail("staff@example.com");
        token.setTokenHash(new BCryptPasswordEncoder().encode("abc-token"));
        token.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(10));
        when(passwordResetTokenRepository.findActiveTokens(any())).thenReturn(java.util.List.of(token));

        var result = passwordResetService.resetPassword("abc-token", "newStrongPassword123!");

        assertTrue(result.isSuccess());
        assertNotEquals("oldhash", staff.getPasswordHash());
        verify(passwordResetTokenRepository).save(any());
        verify(staffRepository).save(staff);
    }
}
