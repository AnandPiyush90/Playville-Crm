package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.invoice.ShareInvoiceRequest;
import com.playville.crm.entity.Branch;
import com.playville.crm.entity.Invoice;
import com.playville.crm.entity.NotificationDelivery;
import com.playville.crm.entity.enums.InvoiceStatus;
import com.playville.crm.exception.BranchAccessDeniedException;
import com.playville.crm.exception.BusinessRuleException;
import com.playville.crm.repository.InvoiceRepository;
import com.playville.crm.repository.NotificationDeliveryRepository;
import com.playville.crm.repository.StaffRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceNotificationServiceTest {
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private NotificationDeliveryRepository deliveryRepository;
    @Mock private InvoiceService invoiceService;
    @Mock private NotificationDeliveryService notificationDeliveryService;
    @Mock private EmailTemplateService emailTemplateService;
    @InjectMocks private InvoiceNotificationService service;

    private Branch branch;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        BranchContext.set(7, "PV7");
        branch = Branch.builder().id(7).branchCode("PV7").branchName("Whitefield")
                .emailSharingEnabled(true).invoiceFromEmail("billing@playville.in").build();
        invoice = Invoice.builder().id(41).branch(branch).status(InvoiceStatus.ISSUED)
                .invoiceNumber("PV7/41").customerNameSnapshot("Asha")
                .customerEmailSnapshot("asha@example.com").customerPhoneSnapshot("9876543210").build();
        when(invoiceRepository.findDetailById(41)).thenReturn(Optional.of(invoice));
        lenient().when(emailTemplateService.render(eq(7),eq(EmailTemplateService.INVOICE),any())).thenReturn(new com.playville.crm.dto.notification.RenderedEmailTemplate("Invoice subject","Invoice body"));
    }

    @AfterEach
    void clearBranchContext() {
        BranchContext.clear();
    }

    @Test
    void sendsEmailPdfAndMarksAuditSent() {
        ShareInvoiceRequest request = request(ShareInvoiceRequest.Channel.EMAIL, null);
        when(deliveryRepository.findByBranchIdAndIdempotencyKey(7, "send-1")).thenReturn(Optional.empty());
        when(invoiceService.document(41)).thenReturn("%PDF-test".getBytes());
        NotificationDelivery queued=NotificationDelivery.builder().id(99).channel("EMAIL").destination("asha@example.com").status("PENDING").build();
        when(notificationDeliveryService.enqueueEmail(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any())).thenReturn(queued);

        var response = service.share(41, request, "staff.user", "send-1");

        assertEquals("PENDING", response.getStatus());
        assertEquals("asha@example.com", response.getDestination());
        verify(notificationDeliveryService).enqueueEmail(any(),any(),any(),eq("INVOICE"),eq("INVOICE"),eq("41"),eq("asha@example.com"),eq("send-1"),any(),any(),any(),any());
    }

    @Test
    void sameIdempotencyKeyReturnsExistingDeliveryWithoutSendingAgain() {
        NotificationDelivery previous = NotificationDelivery.builder().id(55).branch(branch).invoice(invoice)
                .channel("EMAIL").destination("asha@example.com").status("SENT").idempotencyKey("same-key").build();
        when(deliveryRepository.findByBranchIdAndIdempotencyKey(7, "same-key")).thenReturn(Optional.of(previous));

        var response = service.share(41, request(ShareInvoiceRequest.Channel.EMAIL, null), "staff.user", "same-key");

        assertEquals(55, response.getDeliveryId());
        verifyNoInteractions(invoiceService, notificationDeliveryService);
        verify(deliveryRepository, never()).saveAndFlush(any());
    }

    @Test
    void emailDeliveryIsQueuedForDurableDispatch() {
        when(deliveryRepository.findByBranchIdAndIdempotencyKey(7, "disabled-1")).thenReturn(Optional.empty());
        when(invoiceService.document(41)).thenReturn(new byte[] {1});
        NotificationDelivery queued=NotificationDelivery.builder().id(98).channel("EMAIL").destination("asha@example.com").status("PENDING").build();
        when(notificationDeliveryService.enqueueEmail(any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any(),any())).thenReturn(queued);

        var response=service.share(41, request(ShareInvoiceRequest.Channel.EMAIL, null), "staff.user", "disabled-1");

        assertEquals("PENDING",response.getStatus());
        verify(notificationDeliveryService).enqueueEmail(any(),any(),any(),eq("INVOICE"),eq("INVOICE"),eq("41"),eq("asha@example.com"),eq("disabled-1"),any(),any(),any(),any());
    }

    @Test
    void rejectsDraftBeforeCreatingAudit() {
        invoice.setStatus(InvoiceStatus.DRAFT);
        assertThrows(BusinessRuleException.class,
                () -> service.share(41, request(ShareInvoiceRequest.Channel.EMAIL, null), "staff.user", "draft-1"));
        verify(deliveryRepository, never()).saveAndFlush(any());
    }

    @Test
    void preventsCrossBranchInvoiceSharing() {
        BranchContext.set(8, "PV8");
        assertThrows(BranchAccessDeniedException.class,
                () -> service.share(41, request(ShareInvoiceRequest.Channel.EMAIL, null), "staff.user", "cross-1"));
        verify(deliveryRepository, never()).saveAndFlush(any());
    }

    private ShareInvoiceRequest request(ShareInvoiceRequest.Channel channel, String destination) {
        ShareInvoiceRequest request = new ShareInvoiceRequest();
        request.setChannel(channel);
        request.setDestination(destination);
        return request;
    }
}
