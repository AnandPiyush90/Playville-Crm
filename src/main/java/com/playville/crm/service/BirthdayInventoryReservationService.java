package com.playville.crm.service;

import com.playville.crm.entity.*;
import com.playville.crm.entity.enums.InventoryMovementType;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service @RequiredArgsConstructor
public class BirthdayInventoryReservationService {
    private final BirthdayInventoryReservationRepository reservationRepository;
    private final BirthdayQuoteLineRepository quoteLineRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final BranchSkuRepository branchSkuRepository;
    private final InventoryBatchRepository batchRepository;
    private final InventoryMovementRepository movementRepository;

    public void reserve(BirthdayBooking booking) {
        if (reservationRepository.existsByBookingIdAndStatus(booking.getId(), BirthdayInventoryReservation.Status.RESERVED)) return;
        List<BirthdayQuoteLine> lines = quoteLineRepository.findByBirthdayBookingIdOrderByLineNumberAsc(booking.getId()).stream()
                .filter(BirthdayQuoteLine::isInventoryReservationRequired).filter(line -> line.getSku() != null)
                .sorted(Comparator.comparing(line -> line.getSku().getId())).toList();
        for (BirthdayQuoteLine line : lines) {
            ProductSku sku = line.getSku();
            BranchSku branchSku = branchSkuRepository.findByBranchIdAndSkuId(booking.getBranch().getId(), sku.getId())
                    .orElseThrow(() -> new BusinessRuleException("SKU_NOT_AVAILABLE_AT_BRANCH: " + sku.getSkuCode()));
            InventoryBalance balance = balanceRepository.findByBranchIdAndSkuIdForUpdate(booking.getBranch().getId(), sku.getId())
                    .orElseThrow(() -> new ApiConflictException("INSUFFICIENT_STOCK", "No stock available for " + line.getDescriptionSnapshot()));
            if (!branchSku.isAllowNegativeStock() && balance.availableQuantity().compareTo(line.getQuantity()) < 0)
                throw new ApiConflictException("BIRTHDAY_STOCK_SHORTAGE", "Only " + balance.availableQuantity() + " available for " + line.getDescriptionSnapshot());
            balance.setQuantityReserved(balance.getQuantityReserved().add(line.getQuantity())); balanceRepository.save(balance);
            reservationRepository.save(BirthdayInventoryReservation.builder().booking(booking).quoteLine(line).branch(booking.getBranch())
                    .sku(sku).quantity(line.getQuantity()).status(BirthdayInventoryReservation.Status.RESERVED).build());
        }
    }

    public void release(BirthdayBooking booking) {
        for (BirthdayInventoryReservation reservation : reservationRepository.findByBookingIdAndStatus(booking.getId(), BirthdayInventoryReservation.Status.RESERVED)) {
            InventoryBalance balance = balanceRepository.findByBranchIdAndSkuIdForUpdate(booking.getBranch().getId(), reservation.getSku().getId())
                    .orElseThrow(() -> new IllegalStateException("Birthday reservation exists without inventory balance"));
            balance.setQuantityReserved(balance.getQuantityReserved().subtract(reservation.getQuantity())); balanceRepository.save(balance);
            reservation.setStatus(BirthdayInventoryReservation.Status.RELEASED); reservation.setReleasedAt(LocalDateTime.now()); reservationRepository.save(reservation);
        }
    }

    public void consume(BirthdayBooking booking) {
        for (BirthdayInventoryReservation reservation : reservationRepository.findByBookingIdAndStatus(booking.getId(), BirthdayInventoryReservation.Status.RESERVED)) {
            InventoryBalance balance = balanceRepository.findByBranchIdAndSkuIdForUpdate(booking.getBranch().getId(), reservation.getSku().getId())
                    .orElseThrow(() -> new IllegalStateException("Birthday reservation exists without inventory balance"));
            if (balance.getQuantityReserved().compareTo(reservation.getQuantity()) < 0 || balance.getQuantityOnHand().compareTo(reservation.getQuantity()) < 0)
                throw new ApiConflictException("BIRTHDAY_RESERVATION_MISMATCH", "Reserved stock is not available for " + reservation.getQuoteLine().getDescriptionSnapshot());
            balance.setQuantityReserved(balance.getQuantityReserved().subtract(reservation.getQuantity()));
            balance.setQuantityOnHand(balance.getQuantityOnHand().subtract(reservation.getQuantity())); balanceRepository.save(balance);
            consumeBatches(booking, reservation);
            reservation.setStatus(BirthdayInventoryReservation.Status.CONSUMED); reservation.setConsumedAt(LocalDateTime.now()); reservationRepository.save(reservation);
        }
    }

    public String status(Integer bookingId) {
        if (reservationRepository.existsByBookingIdAndStatus(bookingId, BirthdayInventoryReservation.Status.RESERVED)) return "RESERVED";
        if (reservationRepository.existsByBookingIdAndStatus(bookingId, BirthdayInventoryReservation.Status.CONSUMED)) return "CONSUMED";
        if (reservationRepository.existsByBookingIdAndStatus(bookingId, BirthdayInventoryReservation.Status.RELEASED)) return "RELEASED";
        return "NOT_REQUIRED";
    }

    private void consumeBatches(BirthdayBooking booking, BirthdayInventoryReservation reservation) {
        BigDecimal remaining = reservation.getQuantity();
        for (InventoryBatch batch : batchRepository.findSellableForUpdate(booking.getBranch().getId(), reservation.getSku().getId())) {
            if (remaining.signum() <= 0) break;
            BigDecimal quantity = batch.getQuantityRemaining().min(remaining);
            batch.setQuantityRemaining(batch.getQuantityRemaining().subtract(quantity)); batchRepository.save(batch);
            movementRepository.save(movement(booking, reservation, batch, quantity)); remaining = remaining.subtract(quantity);
        }
        if (remaining.signum() > 0) {
            if (reservation.getSku().isHasExpiry()) throw new ApiConflictException("BATCH_EXPIRED", "No valid batch for " + reservation.getSku().getSkuCode());
            movementRepository.save(movement(booking, reservation, null, remaining));
        }
    }

    private InventoryMovement movement(BirthdayBooking booking, BirthdayInventoryReservation reservation, InventoryBatch batch, BigDecimal quantity) {
        return InventoryMovement.builder().branch(booking.getBranch()).sku(reservation.getSku()).batch(batch).movementType(InventoryMovementType.SALE)
                .quantityDelta(quantity.negate()).unitCostSnapshot(batch == null ? reservation.getSku().getDefaultCostPrice() : batch.getUnitCost())
                .referenceType("BIRTHDAY_BOOKING").referenceId(booking.getId()).performedByStaff(booking.getStaff())
                .notes(reservation.getQuoteLine().getDescriptionSnapshot()).build();
    }
}
