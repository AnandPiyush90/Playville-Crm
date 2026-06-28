package com.playville.crm.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LeadSourceConverter implements AttributeConverter<LeadSource, String> {

    @Override
    public String convertToDatabaseColumn(LeadSource source) {
        if (source == null) return null;
        return switch (source) {
            case Walk_in         -> "Walk-in";
            case Friend_Referral -> "Friend Referral";
            default              -> source.name();
        };
    }

    @Override
    public LeadSource convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        return switch (dbValue) {
            case "Walk-in"         -> LeadSource.Walk_in;
            case "Friend Referral" -> LeadSource.Friend_Referral;
            default                -> LeadSource.valueOf(dbValue);
        };
    }
}