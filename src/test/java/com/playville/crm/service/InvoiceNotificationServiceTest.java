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
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceNotificationServiceTest {
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private NotificationDeliveryRepository deliveryRepository;
    @Mock private InvoiceService invoiceService;
    @Mock private JavaMailSender mailSender;
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
    }

    @AfterEach
    void clearBranchContext() {
        BranchContext.clear();
    }

    @Test
    void sendsEmailPdfAndMarksAuditSent() {
        ShareInvoiceRequest request = request(ShareInvoiceRequest.Channel.EMAIL, null);
        when(deliveryRepository.findByBranchIdAndIdempotencyKey(7, "send-1")).thenReturn(Optional.empty());
        when(deliveryRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            NotificationDelivery delivery = invocation.getArgument(0);
            delivery.setId(99);
            return delivery;
        });
        when(invoiceService.document(41)).thenReturn("%PDF-test".getBytes());
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        var response = service.share(41, request, "staff.user", "send-1");

        assertEquals("SENT", response.getStatus());
        assertEquals("asha@example.com", response.getDestination());
        verify(mailSender).send(message);
        verify(deliveryRepository).save(argThat(delivery -> "SENT".equals(delivery.getStatus()) && delivery.getSentAt() != null));
    }

    @Test
    void sameIdempotencyKeyReturnsExistingDeliveryWithoutSendingAgain() {
        NotificationDelivery previous = NotificationDelivery.builder().id(55).branch(branch).invoice(invoice)
                .channel("EMAIL").destination("asha@example.com").status("SENT").idempotencyKey("same-key").build();
        when(deliveryRepository.findByBranchIdAndIdempotencyKey(7, "same-key")).thenReturn(Optional.of(previous));

        var response = service.share(41, request(ShareInvoiceRequest.Channel.EMAIL, null), "staff.user", "same-key");

        assertEquals(55, response.getDeliveryId());
        verifyNoInteractions(mailSender, invoiceService);
        verify(deliveryRepository, never()).saveAndFlush(any());
    }

    @Test
    void disabledChannelKeepsActionableErrorAndFailedAudit() {
        branch.setEmailSharingEnabled(false);
        when(deliveryRepository.findByBranchIdAndIdempotencyKey(7, "disabled-1")).thenReturn(Optional.empty());
        when(deliveryRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceService.document(41)).thenReturn(new byte[] {1});

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.share(41, request(ShareInvoiceRequest.Channel.EMAIL, null), "staff.user", "disabled-1"));

        assertTrue(error.getMessage().contains("EMAIL_SHARING_DISABLED"));
        verify(deliveryRepository).save(argThat(delivery -> "FAILED".equals(delivery.getStatus())
                && delivery.getErrorMessage().contains("EMAIL_SHARING_DISABLED")));
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
