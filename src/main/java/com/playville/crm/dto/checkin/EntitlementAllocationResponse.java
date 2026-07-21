package com.playville.crm.dto.checkin;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EntitlementAllocationResponse {
    private Integer entitlementId;
    private String entitlementType;
    private List<Integer> kidIds;
    private Integer sessionsReserved;
}
