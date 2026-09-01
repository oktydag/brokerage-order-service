package com.brokerage.common.jpa;

import com.brokerage.common.domain.valueobjects.RequestFingerprint;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RequestFingerprintConverter implements AttributeConverter<RequestFingerprint, String> {

    @Override
    public String convertToDatabaseColumn(RequestFingerprint attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public RequestFingerprint convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new RequestFingerprint(dbData);
    }
}
