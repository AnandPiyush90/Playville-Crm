package com.playville.crm.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private final boolean               success = false;
    private final int                   status;
    private final String                error;
    private final String                code;
    private final String                message;
    private final String                path;
    private final LocalDateTime         timestamp;
    private final Map<String, String>   fieldErrors;   // for @Valid failures
}
