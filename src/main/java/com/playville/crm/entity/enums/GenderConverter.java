package com.playville.crm.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender gender) {
        if (gender == null) return null;
        return gender.name();   // Male, Female, Other — match DB exactly
    }

    @Override
    public Gender convertToEntityAttribute(String dbValue) {
        if (dbValue == null) return null;
        return Gender.valueOf(dbValue);
    }
}