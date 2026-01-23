package org.ohdsi.webapi.feanalysis.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ohdsi.webapi.feanalysis.domain.*;
import org.ohdsi.webapi.feanalysis.dto.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class FeAnalysisWithCriteriaEntityToFeAnalysisDTOConverter
        implements Converter<FeAnalysisWithCriteriaEntity<?>, FeAnalysisDTO> {
    private final ObjectMapper objectMapper;
    private final ConversionService conversionService;

    // The @Lazy is necessary. Otherwise the bean is newed up too late and doesn't get attached to the ConversionService, 
    // leading to "No converter found" exceptions
    public FeAnalysisWithCriteriaEntityToFeAnalysisDTOConverter(@Lazy ObjectMapper objectMapper, @Lazy ConversionService conversionService) {
        this.objectMapper = objectMapper;
        this.conversionService = conversionService;
    }

    @Override
    public FeAnalysisDTO convert(final FeAnalysisWithCriteriaEntity<?> source) {
        var dto = new FeAnalysisWithConceptSetDTO();
        BaseFeAnalysisEntityToFeAnalysisDTOConverter.convert(source, dto);
        dto.setDesign(convertDesignToJson(source));
        dto.setConceptSets(source.getConceptSets());
        return dto;
    }

    private Object convertDesignToJson(final FeAnalysisWithCriteriaEntity<?> source) {
        return source.getDesign()
                .stream()
                .map(this::convertCriteria)
                .map(c -> (JsonNode) objectMapper.valueToTree(c))
                .collect(Collectors.toList());
    }

    private BaseFeAnalysisCriteriaDTO convertCriteria(FeAnalysisCriteriaEntity criteriaEntity) {
        BaseFeAnalysisCriteriaDTO criteriaDTO = switch (criteriaEntity) {
            case FeAnalysisCriteriaGroupEntity groupEntity ->
                    new FeAnalysisCriteriaDTO(groupEntity.getId(), groupEntity.getName(),
                            groupEntity.getExpression());
            case FeAnalysisWindowedCriteriaEntity w ->
                    new FeAnalysisWindowedCriteriaDTO(w.getId(), w.getName(), w.getExpression());
            case FeAnalysisDemographicCriteriaEntity d ->
                    new FeAnalysisDemographicCriteriaDTO(d.getId(), d.getName(), d.getExpression());
            case null, default -> throw new IllegalArgumentException(
                    String.format("Cannot convert criteria entity, %s is not supported", criteriaEntity));
        };
        criteriaDTO
                .setAggregate(conversionService.convert(criteriaEntity.getAggregate(), FeAnalysisAggregateDTO.class));
        return criteriaDTO;
    }
}
