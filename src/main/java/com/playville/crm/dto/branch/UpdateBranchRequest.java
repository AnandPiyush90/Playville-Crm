package com.playville.crm.dto.branch;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class UpdateBranchRequest {

    @Size(max = 100)
    private String    branchName;

    private String    address;

    @Size(max = 15)
    private String    phone;

    private LocalTime openTime;
    private LocalTime closeTime;

    @Size(max = 20)
    private String    closedDay;

    @Email
    @Size(max = 150)
    private String    notificationEmail;
}
