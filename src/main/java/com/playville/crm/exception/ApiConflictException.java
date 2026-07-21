package com.playville.crm.exception;

import lombok.Getter;

@Getter
public class ApiConflictException extends RuntimeException {
    private final String code;

    public ApiConflictException(String code, String message) {
        super(message);
        this.code = code;
    }
}
