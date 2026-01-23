package org.ohdsi.webapi.feanalysis.converter;

import jakarta.ws.rs.NotFoundException;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisAggregateEntity;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisAggregateDTO;
import org.ohdsi.webapi.feanalysis.repository.FeAnalysisAggregateRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class FeAnalysisAggregateDTOToEntityConverter
        implements Converter<FeAnalysisAggregateDTO, FeAnalysisAggregateEntity> {

    private final FeAnalysisAggregateRepository aggregateRepository;

    // The @Lazy is necessary. Otherwise the bean is newed up too late and doesn't get attached to the ConversionService, 
    // leading to "No converter found" exceptions
    public FeAnalysisAggregateDTOToEntityConverter(@Lazy FeAnalysisAggregateRepository aggregateRepository) {
        this.aggregateRepository = aggregateRepository;
    }

    @Override
    public FeAnalysisAggregateEntity convert(FeAnalysisAggregateDTO dto) {
        if (Objects.nonNull(dto.getId())) {
            var optionalEntity =  aggregateRepository.findById(dto.getId());
            if (optionalEntity.isEmpty()) {
                throw new NotFoundException("Aggregate with id " + dto.getId() + " not found");
            }
            return optionalEntity.get();
        } else {
            return aggregateRepository.findDefault().orElse(null);
        }
    }
}
