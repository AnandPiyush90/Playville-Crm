package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.entity.BirthdayBooking;
import com.playville.crm.entity.Branch;
import com.playville.crm.exception.BusinessRuleException;
import com.playville.crm.exception.DuplicateResourceException;
import com.playville.crm.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BirthdayBookingServiceTest {
    private final BirthdayBookingRepository bookings = mock(BirthdayBookingRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final KidRepository kids = mock(KidRepository.class);
    private final BranchRepository branches = mock(BranchRepository.class);
    private final StaffRepository staff = mock(StaffRepository.class);
    private final BirthdayQuoteService quotes = mock(BirthdayQuoteService.class);
    private final BirthdayQuoteLineRepository quoteLines = mock(BirthdayQuoteLineRepository.class);
    private final BirthdayCatalogItemRepository catalogItems = mock(BirthdayCatalogItemRepository.class);
    private final BirthdayInventoryReservationService inventoryReservations = mock(BirthdayInventoryReservationService.class);
    private final BirthdayBookingService service = new BirthdayBookingService(
            bookings, customers, kids, branches, staff, quotes, quoteLines, catalogItems, inventoryReservations);

    @AfterEach void clearBranch() { BranchContext.clear(); }

    @Test
    void availabilityRejectsAnOverlappingActiveBooking() {
        BranchContext.set(7, "PV7");
        BirthdayBooking active = BirthdayBooking.builder().id(11).status(BirthdayBooking.BookingStatus.Confirmed).build();
        when(bookings.findByBranchIdAndPartyDateAndPartySlotStartLessThanAndPartySlotEndGreaterThanAndStatusNot(
                eq(7), eq(LocalDate.of(2026, 8, 10)), eq(LocalTime.of(15, 30)), eq(LocalTime.of(13, 0)), eq(BirthdayBooking.BookingStatus.Cancelled)))
                .thenReturn(List.of(active));

        assertFalse(service.isSlotAvailable(LocalDate.of(2026, 8, 10), LocalTime.of(13, 0)));
    }

    @Test
    void availabilityAllowsASlotWhenOnlyAnEnquiryOverlaps() {
        BranchContext.set(7, "PV7");
        BirthdayBooking enquiry = BirthdayBooking.builder().id(12).status(BirthdayBooking.BookingStatus.Enquiry).build();
        when(bookings.findByBranchIdAndPartyDateAndPartySlotStartLessThanAndPartySlotEndGreaterThanAndStatusNot(
                eq(7), any(), any(), any(), eq(BirthdayBooking.BookingStatus.Cancelled))).thenReturn(List.of(enquiry));

        assertTrue(service.isSlotAvailable(LocalDate.of(2026, 8, 10), LocalTime.of(13, 0)));
    }

    @Test
    void confirmationRejectsAnOverlapWithAnExistingConfirmedBooking() {
        BranchContext.set(7, "PV7");
        BirthdayBooking enquiry = BirthdayBooking.builder().id(11).branch(Branch.builder().id(7).build())
                .partyDate(LocalDate.of(2026, 8, 10)).partySlotStart(LocalTime.of(13, 0)).partySlotEnd(LocalTime.of(15, 30))
                .status(BirthdayBooking.BookingStatus.Enquiry).build();
        BirthdayBooking confirmed = BirthdayBooking.builder().id(12).status(BirthdayBooking.BookingStatus.Confirmed).build();
        when(bookings.findById(11)).thenReturn(java.util.Optional.of(enquiry));
        when(bookings.findByBranchIdAndPartyDateAndPartySlotStartLessThanAndPartySlotEndGreaterThanAndStatusNot(
                eq(7), eq(LocalDate.of(2026, 8, 10)), eq(LocalTime.of(15, 30)), eq(LocalTime.of(13, 0)), eq(BirthdayBooking.BookingStatus.Cancelled)))
                .thenReturn(List.of(confirmed));

        assertThrows(DuplicateResourceException.class,
                () -> service.updateStatus(11, BirthdayBooking.BookingStatus.Confirmed));
        verify(bookings, never()).save(any());
    }

    @Test
    void terminalBookingCannotMoveBackToConfirmed() {
        BranchContext.set(7, "PV7");
        BirthdayBooking completed = BirthdayBooking.builder().id(11).branch(Branch.builder().id(7).build())
                .status(BirthdayBooking.BookingStatus.Completed).build();
        when(bookings.findById(11)).thenReturn(java.util.Optional.of(completed));

        assertThrows(BusinessRuleException.class,
                () -> service.updateStatus(11, BirthdayBooking.BookingStatus.Confirmed));
        verify(bookings, never()).save(any());
    }
}
