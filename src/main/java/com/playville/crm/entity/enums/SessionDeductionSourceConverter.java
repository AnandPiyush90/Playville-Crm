package com.playville.crm.entity.enums;

import com.playville.crm.entity.SessionDeduction.DeductionSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SessionDeductionSourceConverter implements AttributeConverter<DeductionSource, String> {

    @Override
    public String convertToDatabaseColumn(DeductionSource source) {
        if (source == null) return null;
        return switch (source) {
            case Checkout -> "Checkout";
            case Auto_Close -> "Auto-Close";
        };
    }

    @Override
    public DeductionSource convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        return switch (dbValue) {
            case "Checkout" -> DeductionSource.Checkout;
            case "Auto-Close" -> DeductionSource.Auto_Close;
            default -> throw new IllegalArgumentException("Unknown deduction source: " + dbValue);
        };
    }
}