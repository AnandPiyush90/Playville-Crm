package com.playville.crm.dto.birthday;

import com.playville.crm.entity.BirthdayBooking;
import com.playville.crm.entity.Purchase;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Builder
public class BirthdayBookingResponse {
    private Integer                    id;
    private Integer                    branchId;
    private String                     branchCode;
    private Integer                    customerId;
    private String                     parentName;
    private Integer                    kidId;
    private String                     kidName;
    private LocalDate                  partyDate;
    private LocalTime                  partySlotStart;
    private LocalTime                  partySlotEnd;
    private Integer                    expectedGuests;
    private Integer                    actualKids;
    private Integer                    actualAdults;
    private Integer                    actualExtraMinutes;
    private String                     completionNotes;
    private String                     cakeOption;
    private Integer                    foodBoxesCount;
    private BigDecimal                 baseAmount;
    private BigDecimal                 discountPct;
    private BigDecimal                 discountAmount;
    private BigDecimal                 gstAmount;
    private BigDecimal                 totalAmount;
    private BigDecimal                 advancePaid;
    private BigDecimal                 balanceDue;
    private Purchase.PaymentMode       paymentMode;
    private String                     paymentReference;
    private BirthdayBooking.BookingStatus status;
    private String                     notes;
    private LocalDateTime              createdAt;
    private String inventoryReservationStatus;
}
