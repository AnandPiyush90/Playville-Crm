package com.playville.crm.dto.birthday;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
public class BirthdayRescheduleRequest {
    @NotNull private LocalDate partyDate;
    @NotNull private LocalTime partySlotStart;
}
