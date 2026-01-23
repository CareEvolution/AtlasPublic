package org.ohdsi.webapi.cohortcharacterization.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.ohdsi.analysis.Utils;
import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.CcExportDTO;
import org.springframework.core.convert.ConversionService;

@Converter
public class SerializedCcToCcConverter implements AttributeConverter<CohortCharacterizationEntity, String> {
    private static ConversionService conversionService;

    public static void setConversionService(ConversionService conversionService) {
        SerializedCcToCcConverter.conversionService = conversionService;
    }

    @Override
    public String convertToDatabaseColumn(CohortCharacterizationEntity data) {
        var cohortCharacterizationDTO = conversionService.convert(data, CcExportDTO.class);
        cohortCharacterizationDTO.setModifiedDate(null);
        return Utils.serialize(cohortCharacterizationDTO);
    }

    @Override
    public CohortCharacterizationEntity convertToEntityAttribute(String data) {
        var typeRef = new TypeReference<CcExportDTO>() {};
        CcExportDTO dto = Utils.deserialize(data, typeRef);
        return conversionService.convert(dto, CohortCharacterizationEntity.class);
    }
}
