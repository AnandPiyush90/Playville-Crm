package com.playville.crm.service;

import com.playville.crm.config.FeatureFlagService;
import com.playville.crm.dto.purchase.PurchaseRequest;
import com.playville.crm.entity.*;
import com.playville.crm.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    private final PurchaseRepository purchases = mock(PurchaseRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final PlayvillePackageRepository packages = mock(PlayvillePackageRepository.class);
    private final BranchRepository branches = mock(BranchRepository.class);
    private final StaffRepository staff = mock(StaffRepository.class);
    private final CheckinRepository checkins = mock(CheckinRepository.class);
    private final CustomerEntitlementRepository entitlements = mock(CustomerEntitlementRepository.class);
    private final FeatureFlagService featureFlags = new FeatureFlagService(true);
    private final InvoiceService invoiceService = mock(InvoiceService.class);
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);

    private final PurchaseService service = new PurchaseService(
            purchases, customers, packages, branches, staff, checkins, entitlements, featureFlags,
            invoiceService, invoices);

    @Test
    void purchasePackageReplaysExistingPurchaseForIdempotencyKey() {
        Customer customer = Customer.builder().id(20).phoneNumber("9999999999").parentName("Parent").build();
        PlayvillePackage pkg = PlayvillePackage.builder().id(30).packageName("Ten Sessions")
                .sessionsPurchased(10).pricePerSession(BigDecimal.TEN).totalPrice(new BigDecimal("100.00")).build();
        Purchase purchase = Purchase.builder().id(40).customer(customer).playvillePackage(pkg)
                .sessionsAdded(10).amountPaid(new BigDecimal("100.00")).gstAmount(new BigDecimal("18.00"))
                .discountApplied(BigDecimal.ZERO).balanceBefore(0).balanceAfter(10)
                .paymentMode(Purchase.PaymentMode.UPI).idempotencyKey("purchase-key").build();
        PurchaseRequest request = new PurchaseRequest();
        request.setCustomerId(20);
        request.setPackageId(30);
        request.setPaymentMode(Purchase.PaymentMode.UPI);

        when(purchases.findByIdempotencyKey("purchase-key")).thenReturn(Optional.of(purchase));
        when(invoices.findByPurchaseId(40)).thenReturn(Optional.empty());

        var response = service.purchasePackage(request, "admin", "purchase-key");

        assertEquals(40, response.getId());
        assertEquals(10, response.getBalanceAfter());
        verifyNoInteractions(customers, packages, branches, staff, checkins, entitlements);
        verify(invoiceService).ensurePurchaseReceipt(purchase, "admin");
        verify(purchases, never()).save(any());
    }
}
