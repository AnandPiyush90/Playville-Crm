package com.playville.crm.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String  token;
    private String  username;
    private String  role;
    private Integer branchId;
    private String  branchCode;
    private String  branchName;
    private long    expiresInMs;
}