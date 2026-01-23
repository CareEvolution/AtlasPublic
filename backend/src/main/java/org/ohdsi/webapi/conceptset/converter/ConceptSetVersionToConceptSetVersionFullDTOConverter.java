package org.ohdsi.webapi.conceptset.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.NotFoundException;

import org.ohdsi.webapi.conceptset.ConceptSetItem;
import org.ohdsi.webapi.conceptset.ConceptSetRepository;
import org.ohdsi.webapi.conceptset.dto.ConceptSetDTO;
import org.ohdsi.webapi.conceptset.dto.ConceptSetVersionFullDTO;
import org.ohdsi.webapi.versioning.VersionToVersionDTOConverter;
import org.ohdsi.webapi.versioning.domain.ConceptSetVersion;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConceptSetVersionToConceptSetVersionFullDTOConverter implements Converter<ConceptSetVersion, ConceptSetVersionFullDTO> {

    private final ConceptSetRepository conceptSetRepository;
    private final ObjectMapper mapper;
    private final ConversionService conversionService;

    public ConceptSetVersionToConceptSetVersionFullDTOConverter(
        @Lazy ConceptSetRepository conceptSetRepository,
        @Lazy ObjectMapper mapper,
        @Lazy ConversionService conversionService
    ) {
        this.conceptSetRepository = conceptSetRepository;
        this.mapper = mapper;
        this.conversionService = conversionService;
    }

    @Override
    public ConceptSetVersionFullDTO convert(ConceptSetVersion source) {
        List<ConceptSetItem> items;
        try {
            items = mapper.readValue(source.getAssetJson(),
                    new TypeReference<List<ConceptSetItem>>() {
                    });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        var optionalConceptSet = conceptSetRepository.findById(source.getAssetId().intValue());
        if (optionalConceptSet.isEmpty()) {
            throw new NotFoundException(String.format("There is no concept set with id = %d.", source.getAssetId()));
        }
        var conceptSet = optionalConceptSet.get();

        var target = new ConceptSetVersionFullDTO();
        target.setItems(items);
        target.setVersionDTO(VersionToVersionDTOConverter.convert(source));
        target.setEntityDTO(conversionService.convert(conceptSet, ConceptSetDTO.class));

        return target;
    }
}
