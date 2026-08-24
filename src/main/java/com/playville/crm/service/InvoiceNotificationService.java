package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.invoice.*;
import com.playville.crm.entity.*;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.*;
import org.springframework.web.client.RestClient;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class InvoiceNotificationService {
    private final InvoiceRepository invoiceRepository;
    private final StaffRepository staffRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final InvoiceService invoiceService;
    private final NotificationDeliveryService notificationDeliveryService;
    private final EmailTemplateService emailTemplateService;

    @Value("${app.notifications.whatsapp.access-token:}") private String whatsappAccessToken;
    @Value("${app.notifications.whatsapp.graph-url:https://graph.facebook.com}") private String whatsappGraphUrl;
    @Value("${app.notifications.whatsapp.api-version:v22.0}") private String whatsappApiVersion;

    @Transactional
    public ShareInvoiceResponse share(Integer invoiceId, ShareInvoiceRequest request, String username, String idempotencyKey) {
        Invoice invoice = invoiceRepository.findDetailById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        if (!invoice.getBranch().getId().equals(BranchContext.getBranchId())) throw new BranchAccessDeniedException();
        if (blank(idempotencyKey)) throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED: Idempotency-Key header is required");
        Optional<NotificationDelivery> previous = deliveryRepository.findByBranchIdAndIdempotencyKey(invoice.getBranch().getId(), idempotencyKey);
        if (previous.isPresent()) return response(previous.get());
        if (invoice.getStatus() == com.playville.crm.entity.enums.InvoiceStatus.DRAFT)
            throw new BusinessRuleException("INVOICE_NOT_SHAREABLE: Complete the draft before sharing it");
        Branch branch = invoice.getBranch();
        Staff staff = staffRepository.findByUsernameAndIsActiveTrue(username).orElse(null);
        ShareInvoiceRequest.Channel channel = request.getChannel() == null ? ShareInvoiceRequest.Channel.EMAIL : request.getChannel();
        String destination = normalizeDestination(channel, request.getDestination(), invoice);
        if(channel==ShareInvoiceRequest.Channel.EMAIL){
            String branchName=firstNonBlank(branch.getBranchName(),"PlayVille");
            String customerName=firstNonBlank(invoice.getCustomerNameSnapshot(),"Customer");
            String number=firstNonBlank(invoice.getInvoiceNumber(),"Invoice "+invoice.getId());
            byte[] pdf=invoiceService.document(invoiceId);
            var template=emailTemplateService.render(branch.getId(),EmailTemplateService.INVOICE,Map.of("CUSTOMER_NAME",customerName,"INVOICE_NUMBER",number,"BRANCH_NAME",branchName));
            NotificationDelivery delivery=notificationDeliveryService.enqueueEmail(branch,invoice.getCustomer(),staff,"INVOICE","INVOICE",String.valueOf(invoice.getId()),destination,idempotencyKey,template.subject(),
                    template.bodyText(),pdf,"playville-invoice-"+invoice.getId()+".pdf");
            return response(delivery);
        }
        NotificationDelivery delivery = deliveryRepository.saveAndFlush(NotificationDelivery.builder().branch(branch).invoice(invoice)
                .customer(invoice.getCustomer()).triggeredByStaff(staff).channel(channel.name()).destination(destination)
                .status("PENDING").idempotencyKey(idempotencyKey).build());
        try {
            byte[] pdf = invoiceService.document(invoiceId);
            String providerReference = sendWhatsapp(branch, invoice, destination, pdf);
            delivery.setStatus("SENT"); delivery.setProviderReference(providerReference); delivery.setSentAt(LocalDateTime.now());
        } catch (BusinessRuleException exception) {
            delivery.setStatus("FAILED"); delivery.setErrorMessage(limit(exception.getMessage(), 4000)); deliveryRepository.save(delivery);
            throw exception;
        } catch (Exception exception) {
            delivery.setStatus("FAILED"); delivery.setErrorMessage(limit(exception.getMessage(), 4000)); deliveryRepository.save(delivery);
            throw new ApiConflictException("INVOICE_DELIVERY_FAILED", "Invoice delivery failed. Check branch notification settings and try again");
        }
        deliveryRepository.save(delivery);
        return response(delivery);
    }

    @SuppressWarnings("unchecked")
    private String sendWhatsapp(Branch branch, Invoice invoice, String destination, byte[] pdf) {
        if (!branch.isWhatsappSharingEnabled()) throw new BusinessRuleException("WHATSAPP_SHARING_DISABLED: Enable WhatsApp sharing in Branch Settings");
        if (blank(branch.getWhatsappPhoneNumberId()) || blank(whatsappAccessToken)) throw new BusinessRuleException("WHATSAPP_CONFIGURATION_REQUIRED: Configure the phone number ID and server access token");
        RestClient client = RestClient.builder()
                .baseUrl(whatsappGraphUrl + "/" + whatsappApiVersion + "/" + branch.getWhatsappPhoneNumberId())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + whatsappAccessToken)
                .build();
        MultipartBodyBuilder body = new MultipartBodyBuilder(); body.part("messaging_product", "whatsapp"); body.part("type", MediaType.APPLICATION_PDF_VALUE);
        body.part("file", new ByteArrayResource(pdf) { @Override public String getFilename() { return "playville-invoice-" + invoice.getId() + ".pdf"; } }).contentType(MediaType.APPLICATION_PDF);
        Map<String,Object> upload = client.post().uri("/media").contentType(MediaType.MULTIPART_FORM_DATA).body(body.build()).retrieve().body(Map.class);
        String mediaId = upload == null ? null : String.valueOf(upload.get("id")); if (blank(mediaId)) throw new IllegalStateException("WhatsApp media upload returned no media ID");
        Map<String,Object> payload = whatsappPayload(branch, invoice, destination, mediaId);
        Map<String,Object> response = client.post().uri("/messages").contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(Map.class);
        Object messages = response == null ? null : response.get("messages");
        if (messages instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof Map<?,?> item) return String.valueOf(item.get("id"));
        return mediaId;
    }

    private Map<String,Object> whatsappPayload(Branch branch, Invoice invoice, String to, String mediaId) {
        String template = branch.getWhatsappInvoiceTemplateName();
        if (!blank(template)) {
            Map<String, Object> document = Map.of(
                    "id", mediaId,
                    "filename", "playville-invoice-" + invoice.getId() + ".pdf");
            Map<String, Object> parameter = Map.of(
                    "type", "document",
                    "document", document);
            Map<String, Object> component = Map.of(
                    "type", "header",
                    "parameters", List.of(parameter));
            Map<String, Object> templatePayload = Map.of(
                    "name", template,
                    "language", Map.of("code", firstNonBlank(branch.getWhatsappLanguageCode(), "en")),
                    "components", List.of(component));
            return Map.of(
                    "messaging_product", "whatsapp",
                    "to", to,
                    "type", "template",
                    "template", templatePayload);
        }
        return Map.of("messaging_product", "whatsapp", "to", to, "type", "document", "document", Map.of("id", mediaId, "filename", "playville-invoice-" + invoice.getId() + ".pdf", "caption", "Your PlayVille invoice " + firstNonBlank(invoice.getInvoiceNumber(), "#" + invoice.getId())));
    }

    private String normalizeDestination(ShareInvoiceRequest.Channel channel, String requested, Invoice invoice) {
        String value = firstNonBlank(requested, channel == ShareInvoiceRequest.Channel.EMAIL ? invoice.getCustomerEmailSnapshot() : invoice.getCustomerPhoneSnapshot());
        if (value == null) throw new BusinessRuleException("DELIVERY_DESTINATION_REQUIRED: Customer " + (channel == ShareInvoiceRequest.Channel.EMAIL ? "email" : "mobile number") + " is not available");
        if (channel == ShareInvoiceRequest.Channel.EMAIL && !value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new BusinessRuleException("INVALID_EMAIL: Enter a valid email address");
        if (channel == ShareInvoiceRequest.Channel.WHATSAPP) { value = value.replaceAll("[^0-9]", ""); if (value.length() == 10) value = "91" + value; if (value.length() < 10 || value.length() > 15) throw new BusinessRuleException("INVALID_WHATSAPP_NUMBER: Enter a mobile number with country code"); }
        return value;
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String firstNonBlank(String... values) { for (String v : values) if (!blank(v)) return v.trim(); return null; }
    private String limit(String value, int max) { return value == null ? "Unknown provider error" : value.substring(0, Math.min(value.length(), max)); }
    private ShareInvoiceResponse response(NotificationDelivery delivery) {
        return ShareInvoiceResponse.builder().deliveryId(delivery.getId()).channel(delivery.getChannel())
                .destination(delivery.getDestination()).status(delivery.getStatus()).build();
    }
}
