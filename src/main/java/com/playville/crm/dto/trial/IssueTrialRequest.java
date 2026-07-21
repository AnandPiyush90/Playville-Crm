package com.playville.crm.dto.trial;
import jakarta.validation.constraints.NotEmpty; import lombok.Getter; import lombok.Setter; import java.util.List;
@Getter @Setter public class IssueTrialRequest { @NotEmpty private List<Integer> kidIds; private String campaignCode; private String notes; }
