package com.playville.crm.dto.trial;
import lombok.*; import java.util.List;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TrialEligibilityResponse { private boolean eligible; private String scope; private List<Integer> eligibleKidIds; private String reasonCode; private String reason; private Integer existingEntitlementId; }
