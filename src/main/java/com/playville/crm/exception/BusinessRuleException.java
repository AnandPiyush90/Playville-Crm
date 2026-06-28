package com.playville.crm.exception;

// For domain violations: kid too old, zero balance checkout, etc.
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}