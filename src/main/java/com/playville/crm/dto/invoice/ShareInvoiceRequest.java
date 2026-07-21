package com.playville.crm.dto.invoice;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShareInvoiceRequest {
    public enum Channel { EMAIL, WHATSAPP }
    private Channel channel = Channel.EMAIL;
    @Size(max = 150)
    private String destination;
}
