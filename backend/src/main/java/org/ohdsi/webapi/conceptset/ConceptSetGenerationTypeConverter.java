/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ohdsi.webapi.conceptset;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Objects;

@Converter(autoApply = true)
public class ConceptSetGenerationTypeConverter implements AttributeConverter<ConceptSetGenerationType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(ConceptSetGenerationType status) {
        if (Objects.requireNonNull(status) == ConceptSetGenerationType.NEGATIVE_CONTROLS) {
            return 0;
        }
        throw new IllegalArgumentException("Unknown value: " + status);
    }

    @Override
    public ConceptSetGenerationType convertToEntityAttribute(Integer statusValue) {
        if (statusValue == 0) {
            return ConceptSetGenerationType.NEGATIVE_CONTROLS;
        }
        throw new IllegalArgumentException("Unknown status value: " + statusValue);
    }
}
