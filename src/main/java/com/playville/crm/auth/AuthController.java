package com.playville.crm.auth;

import com.playville.crm.auth.dto.ForgotPasswordRequest;
import com.playville.crm.auth.dto.ResetPasswordRequest;
import com.playville.crm.auth.service.PasswordResetService;
import com.playville.crm.common.ApiResponse;
import com.playville.crm.exception.ResourceNotFoundException;
import com.playville.crm.security.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@Tag(name = "Authentication", description = "Staff login and token management")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider      jwtTokenProvider;
    private final PasswordEncoder       passwordEncoder;
    private final Environment           environment;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "Staff login", description = "Returns JWT for branch-scoped access")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));

        StaffPrincipal principal = (StaffPrincipal) auth.getPrincipal();
        String token = jwtTokenProvider.generateToken(principal);

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .username(principal.getUsername())
                .role(principal.getRole())
                .branchId(principal.getBranchId())
                .branchCode(principal.getBranchCode())
                .expiresInMs(86400000L)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @Operation(summary = "Request password reset", description = "Sends a reset email for a registered staff account")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        ApiResponse<Void> response = passwordResetService.requestPasswordReset(request.getEmail(), resolveClientIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reset password using token", description = "Validates the reset token and updates the staff password")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ApiResponse<Void> response = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Token check", description = "Returns current staff info from JWT")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> me(
            @CurrentStaff StaffPrincipal principal) {

        LoginResponse response = LoginResponse.builder()
                .username(principal.getUsername())
                .role(principal.getRole())
                .branchId(principal.getBranchId())
                .branchCode(principal.getBranchCode())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    // ─── Dev/QA only — never exposed in production ──────────────
    @Operation(summary = "Generate BCrypt hash (dev/qa only)")
    @GetMapping("/hash")
    public ResponseEntity<String> hash(@RequestParam String password) {
        boolean productionProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod"));
        if (productionProfile) {
            throw new ResourceNotFoundException("Endpoint not found");
        }
        return ResponseEntity.ok(passwordEncoder.encode(password));
    }
    // ─────────────────────────────────────────────────────────────
}
