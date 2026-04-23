package com.example.cellex.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.UUID;

@Converter(autoApply = false)
public class StringUuidConverter implements AttributeConverter<String, UUID> {

    @Override
    public UUID convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(attribute);
        } catch (IllegalArgumentException e) {
            // Trả về null hoặc xử lý nếu chuỗi không phải định dạng UUID
            return null;
        }
    }

    @Override
    public String convertToEntityAttribute(UUID dbData) {
        return (dbData == null) ? null : dbData.toString();
    }
}