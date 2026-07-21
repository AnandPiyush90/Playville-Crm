package com.playville.crm.auth;

import com.playville.crm.common.ApiResponse;
import com.playville.crm.exception.ResourceNotFoundException;
import com.playville.crm.security.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    // ─── TEMPORARY — remove after first successful login ─────────
    @Operation(summary = "Generate BCrypt hash — REMOVE BEFORE PRODUCTION")
    @GetMapping("/hash")
    public ResponseEntity<String> hash(@RequestParam String password) {
        boolean localProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("dev") || profile.equals("local"));
        if (!localProfile) {
            throw new ResourceNotFoundException("Endpoint not found");
        }
        return ResponseEntity.ok(passwordEncoder.encode(password));
    }
    // ─────────────────────────────────────────────────────────────
}
