package org.ohdsi.webapi.source;

import org.ohdsi.webapi.util.EncryptionUtils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SecretAttributeConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return EncryptionUtils.encrypt(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting secret", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return EncryptionUtils.decrypt(dbData);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting secret", e);
        }
    }
}
