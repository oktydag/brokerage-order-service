package com.brokerage.common.jpa;

import com.brokerage.common.domain.AssetName;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AssetNameConverter implements AttributeConverter<AssetName, String> {

    @Override
    public String convertToDatabaseColumn(AssetName attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public AssetName convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AssetName.of(dbData);
    }
}
