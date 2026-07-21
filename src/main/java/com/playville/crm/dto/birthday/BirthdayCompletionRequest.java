package com.playville.crm.dto.birthday;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BirthdayCompletionRequest {
    @Min(0) private Integer actualKids;
    @Min(0) private Integer actualAdults;
    @Min(0) private Integer actualExtraMinutes;
    @Size(max = 2000) private String completionNotes;
}
