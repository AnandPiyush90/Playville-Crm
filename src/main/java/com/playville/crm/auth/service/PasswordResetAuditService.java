package com.playville.crm.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PasswordResetAuditService {

    public void logPasswordResetRequest(String email, String ipAddress, String outcome) {
        log.info("PASSWORD_RESET_REQUEST email={} ip={} outcome={}", redactEmail(email), ipAddress, outcome);
    }

    public void logPasswordResetSuccess(Integer staffId, String email) {
        log.info("PASSWORD_RESET_SUCCESS staffId={} email={}", staffId, redactEmail(email));
    }

    public void logExpiredTokenAttempt(String tokenHint, String email) {
        log.warn("PASSWORD_RESET_EXPIRED_TOKEN email={} tokenHint={}", redactEmail(email), tokenHint);
    }

    public void logInvalidTokenAttempt(String tokenHint) {
        log.warn("PASSWORD_RESET_INVALID_TOKEN tokenHint={}", tokenHint);
    }

    public void logThrottled(String email, String ipAddress) {
        log.warn("PASSWORD_RESET_THROTTLED email={} ip={}", redactEmail(email), ipAddress);
    }

    private String redactEmail(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        String local = email.substring(0, Math.min(2, at));
        String domain = at < email.length() - 1 ? email.substring(at) : "";
        return local + "***" + domain;
    }
}
