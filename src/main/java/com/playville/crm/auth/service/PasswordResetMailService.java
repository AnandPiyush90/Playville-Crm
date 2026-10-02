package com.playville.crm.auth.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.exception.BusinessRuleException;
import com.playville.crm.service.BranchEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class PasswordResetMailService {

    private final BranchEmailService branchEmailService;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Value("${app.password-reset.ttl-minutes:15}")
    private int tokenTtlMinutes;

    public void sendPasswordResetEmail(Integer branchId, String email, String token) {
        Integer effectiveBranchId = branchId != null ? branchId : BranchContext.getBranchId();
        if (effectiveBranchId == null) {
            throw new BusinessRuleException("EMAIL_CONFIGURATION_REQUIRED: Branch context is required to send password reset emails");
        }

        String resetUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
            .path("/reset-password")
            .queryParam("token", token)
            .build()
            .toUriString();

        String body = "Hello,\n\n"
                + "We received a request to reset the password for your PlayVille CRM account.\n\n"
                + "Reset your password using this secure link:\n"
                + resetUrl + "\n\n"
                + "This link expires in " + tokenTtlMinutes + " minutes and can be used only once.\n\n"
                + "If you did not request a password reset, you can safely ignore this email. "
                + "Your password will not change unless the link is used.\n\n"
                + "Regards,\n"
                + "PlayVille CRM Support";

        branchEmailService.send(effectiveBranchId, email, "Reset your PlayVille CRM password", body, null);
    }
}
