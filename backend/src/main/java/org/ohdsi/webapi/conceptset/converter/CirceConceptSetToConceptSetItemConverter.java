package org.ohdsi.webapi.conceptset.converter;

import org.ohdsi.webapi.conceptset.ConceptSetItem;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CirceConceptSetToConceptSetItemConverter<T extends org.ohdsi.circe.vocabulary.ConceptSetExpression.ConceptSetItem>
        implements Converter<T, ConceptSetItem> {
    public ConceptSetItem convert(T source) {
        ConceptSetItem csi = new ConceptSetItem();
        csi.setConceptId(source.concept.conceptId);
        csi.setIncludeDescendants(source.includeDescendants ? 1 : 0);
        csi.setIncludeMapped(source.includeMapped ? 1 : 0);
        csi.setIsExcluded(source.isExcluded ? 1 : 0);
        return csi;
    }
}
