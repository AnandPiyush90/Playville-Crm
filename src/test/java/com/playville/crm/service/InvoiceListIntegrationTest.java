package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class InvoiceListIntegrationTest {
    @Autowired private InvoiceService invoiceService;
    @Autowired private PurchaseService purchaseService;

    @AfterEach void clearBranch() { BranchContext.clear(); }

    @Test
    void listsInvoicesForBranchOneWithoutFilters() {
        BranchContext.set(1, "PV1");
        assertDoesNotThrow(() -> invoiceService.list(null, null, null, null, null, 0, 20));
    }

    @Test
    void listsPurchasesForBranchOneWithoutFilters() {
        BranchContext.set(1, "PV1");
        assertDoesNotThrow(() -> purchaseService.getBranchPurchases(0, 20));
    }

    @Test
    void generatesARealPdfForAnExistingInvoice() {
        BranchContext.set(1, "PV1");
        var invoices = invoiceService.list(null, null, null, null, null, 0, 1);
        if (invoices.isEmpty()) return;
        byte[] pdf = invoiceService.document(invoices.getContent().getFirst().getId());
        assertTrue(pdf.length > 100, "Generated PDF should not be empty");
        assertEquals("%PDF", new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));
    }
}
