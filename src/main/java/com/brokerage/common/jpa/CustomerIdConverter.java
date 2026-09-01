package com.brokerage.common.jpa;

import com.brokerage.common.domain.CustomerId;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CustomerIdConverter implements AttributeConverter<CustomerId, String> {

    @Override
    public String convertToDatabaseColumn(CustomerId attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public CustomerId convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CustomerId.of(dbData);
    }
}
