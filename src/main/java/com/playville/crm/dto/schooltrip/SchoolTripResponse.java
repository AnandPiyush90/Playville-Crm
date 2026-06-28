package com.playville.crm.dto.schooltrip;

import com.playville.crm.entity.BirthdayBooking;
import com.playville.crm.entity.Purchase;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Builder
public class SchoolTripResponse {
    private Integer                       id;
    private Integer                       branchId;
    private String                        branchCode;
    private String                        schoolName;
    private String                        contactPerson;
    private String                        contactPhone;
    private String                        contactEmail;
    private LocalDate                     tripDate;
    private LocalTime                     slotStart;
    private LocalTime                     slotEnd;
    private Integer                       expectedKids;
    private Integer                       actualKids;
    private BigDecimal                    pricePerKid;
    private BigDecimal                    totalAmount;
    private BigDecimal                    gstAmount;
    private BigDecimal                    advancePaid;
    private BigDecimal                    balanceDue;
    private Purchase.PaymentMode          paymentMode;
    private String                        paymentReference;
    private BirthdayBooking.BookingStatus status;
    private String                        notes;
    private LocalDateTime                 createdAt;
}