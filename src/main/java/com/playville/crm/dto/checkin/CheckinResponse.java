package com.playville.crm.dto.checkin;

import com.playville.crm.dto.kid.KidDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
public class CheckinResponse {
    private Integer         checkinId;
    private Integer         customerId;
    private String          parentName;
    private String          phoneNumber;
    private Integer         branchId;
    private String          branchCode;
    private LocalDateTime   checkinTime;
    private String          status;
    private Integer         kidsCount;
    private List<KidDto>    kids;
    private Integer         sessionBalance;
}