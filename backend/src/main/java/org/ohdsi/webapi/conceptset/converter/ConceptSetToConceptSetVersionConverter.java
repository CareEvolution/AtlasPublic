package org.ohdsi.webapi.conceptset.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ohdsi.webapi.conceptset.ConceptSet;
import org.ohdsi.webapi.conceptset.ConceptSetItem;
import org.ohdsi.webapi.conceptset.ConceptSetItemRepository;
import org.ohdsi.webapi.versioning.domain.ConceptSetVersion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConceptSetToConceptSetVersionConverter {

    private final ConceptSetItemRepository conceptSetItemRepository;
    private final ObjectMapper mapper;

    public ConceptSetToConceptSetVersionConverter(ConceptSetItemRepository conceptSetItemRepository, ObjectMapper mapper) {
        this.conceptSetItemRepository = conceptSetItemRepository;
        this.mapper = mapper;
    }

    public ConceptSetVersion convert(ConceptSet source) {
        List<ConceptSetItem> items = conceptSetItemRepository.findAllByConceptSetId(source.getId());
        String itemString;
        try {
            itemString = mapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ConceptSetVersion target = new ConceptSetVersion();
        target.setAssetId(source.getId());
        target.setAssetJson(itemString);

        return target;
    }
}
