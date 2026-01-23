package org.ohdsi.webapi.feanalysis.converter;

import org.ohdsi.analysis.Utils;
import org.ohdsi.analysis.cohortcharacterization.design.CcResultType;
import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType;
import org.ohdsi.webapi.feanalysis.domain.*;
import org.ohdsi.webapi.feanalysis.dto.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// TODO fix raw use of parameterized classes FeAnalysisWithCriteriaEntity, List
@Component
public class FeAnalysisDTOToFeAnalysisWithCriteriasConverter implements
        Converter<FeAnalysisDTO, FeAnalysisWithCriteriaEntity<?>> {

    private static final String RESULT_TYPE_IS_NOT_SUPPORTED = "Result type of %s is not supported";
    private static final String DTO_IS_NOT_SUPPORTED = "DTO class is not supported";

    private final ConversionService conversionService;

    // The @Lazy is necessary. Otherwise the bean is newed up too late and doesn't get attached to the ConversionService, 
    // leading to "No converter found" exceptions
    public FeAnalysisDTOToFeAnalysisWithCriteriasConverter(@Lazy ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public FeAnalysisWithCriteriaEntity<?> convert(final FeAnalysisDTO source) {
        final var baseEntity = createResultObject(source);
        BaseFeAnalysisDTOToFeAnalysisConverter.convert(source, baseEntity);
        baseEntity.setStatType(source.getStatType());
        var list = getBuilder(source.getStatType()).buildList(source.getDesign());
        baseEntity.setDesign(list);
        baseEntity.getDesign().forEach(c -> c.setFeatureAnalysis(baseEntity));
        if (Objects.equals(StandardFeatureAnalysisType.CRITERIA_SET, source.getType())) {
            convert(baseEntity, (FeAnalysisWithConceptSetDTO) source);
        }
        return baseEntity;
    }

    private void convert(FeAnalysisWithCriteriaEntity<?> baseEntity, FeAnalysisWithConceptSetDTO source) {
        var conceptSetEntity = new FeAnalysisConcepsetEntity();
        conceptSetEntity.setFeatureAnalysis(baseEntity);
        conceptSetEntity.setRawExpression(Utils.serialize(source.getConceptSets()));
        baseEntity.setConceptSetEntity(conceptSetEntity);
    }

    protected FeAnalysisWithCriteriaEntity<?> createResultObject(FeAnalysisDTO source) {
        return getBuilder(source.getStatType()).createFeAnalysisObject();
    }

    interface FeAnalysisBuilder<T extends FeAnalysisCriteriaEntity> {
        FeAnalysisWithCriteriaEntity<T> createFeAnalysisObject();

        List<T> buildList(final Object design);
    }

    FeAnalysisBuilder getBuilder(CcResultType statType) {
        if (Objects.equals(CcResultType.PREVALENCE, statType)) {
            return new FeAnalysisPrevalenceCriteriaBuilder(conversionService);
        } else if (Objects.equals(CcResultType.DISTRIBUTION, statType)) {
            return new FeAnalysisDistributionCriteriaBuilder(conversionService);
        }
        throw new IllegalArgumentException(String.format(RESULT_TYPE_IS_NOT_SUPPORTED, statType));
    }

    static abstract class FeAnalysisBuilderSupport<T extends FeAnalysisCriteriaEntity> implements FeAnalysisBuilder<T> {
        private ConversionService conversionService;

        public FeAnalysisBuilderSupport(ConversionService conversionService) {
            this.conversionService = conversionService;
        }

        public List<T> buildList(final Object design) {
            var result = new ArrayList<T>();
            if (!(design instanceof List<?>)) {
                throw new IllegalArgumentException(
                        "Design: " + design.toString() + " cannot be converted to Criteria List");
            } else {
                for (final Object criteria : (List<?>) design) {
                    if (!(criteria instanceof BaseFeAnalysisCriteriaDTO)) {
                        throw new IllegalArgumentException(
                                "Object " + criteria.toString() + " cannot be converted to Criteria");
                    } else {
                        final BaseFeAnalysisCriteriaDTO typifiedCriteria = (BaseFeAnalysisCriteriaDTO) criteria;
                        final T criteriaEntity = newCriteriaEntity(typifiedCriteria);
                        criteriaEntity.setExpressionString(Utils.serialize(getExpression(typifiedCriteria)));
                        criteriaEntity.setId(typifiedCriteria.getId());
                        criteriaEntity.setName(typifiedCriteria.getName());
                        criteriaEntity.setAggregate(conversionService.convert(typifiedCriteria.getAggregate(),
                                FeAnalysisAggregateEntity.class));
                        result.add(criteriaEntity);
                    }
                }
            }
            return result;
        }

        protected abstract Object getExpression(BaseFeAnalysisCriteriaDTO typifiedCriteria);

        protected abstract T newCriteriaEntity(BaseFeAnalysisCriteriaDTO typifiedCriteria);
    }

    static class FeAnalysisPrevalenceCriteriaBuilder extends FeAnalysisBuilderSupport<FeAnalysisCriteriaGroupEntity> {
        public FeAnalysisPrevalenceCriteriaBuilder(ConversionService conversionService) {
            super(conversionService);
        }

        @Override
        public FeAnalysisWithCriteriaEntity<FeAnalysisCriteriaGroupEntity> createFeAnalysisObject() {
            return new FeAnalysisWithPrevalenceCriteriaEntity();
        }

        @Override
        protected Object getExpression(BaseFeAnalysisCriteriaDTO typifiedCriteria) {
            if (typifiedCriteria instanceof FeAnalysisCriteriaDTO) {
                return ((FeAnalysisCriteriaDTO) typifiedCriteria).getExpression();
            }
            return null;
        }

        @Override
        protected FeAnalysisCriteriaGroupEntity newCriteriaEntity(BaseFeAnalysisCriteriaDTO criteriaDTO) {
            return new FeAnalysisCriteriaGroupEntity();
        }
    }

    static class FeAnalysisDistributionCriteriaBuilder
            extends FeAnalysisBuilderSupport<FeAnalysisDistributionCriteriaEntity> {
        public FeAnalysisDistributionCriteriaBuilder(ConversionService conversionService) {
            super(conversionService);
        }

        @Override
        public FeAnalysisWithCriteriaEntity<FeAnalysisDistributionCriteriaEntity> createFeAnalysisObject() {
            return new FeAnalysisWithDistributionCriteriaEntity();
        }

        @Override
        protected Object getExpression(BaseFeAnalysisCriteriaDTO typifiedCriteria) {
            if (typifiedCriteria instanceof FeAnalysisWindowedCriteriaDTO) {
                return ((FeAnalysisWindowedCriteriaDTO) typifiedCriteria).getExpression();
            } else if (typifiedCriteria instanceof FeAnalysisDemographicCriteriaDTO) {
                return ((FeAnalysisDemographicCriteriaDTO) typifiedCriteria).getExpression();
            }
            throw new IllegalArgumentException(DTO_IS_NOT_SUPPORTED);
        }

        @Override
        protected FeAnalysisDistributionCriteriaEntity newCriteriaEntity(BaseFeAnalysisCriteriaDTO criteriaDTO) {
            if (criteriaDTO instanceof FeAnalysisWindowedCriteriaDTO) {
                return new FeAnalysisWindowedCriteriaEntity();
            } else if (criteriaDTO instanceof FeAnalysisDemographicCriteriaDTO) {
                return new FeAnalysisDemographicCriteriaEntity();
            }
            throw new IllegalArgumentException(DTO_IS_NOT_SUPPORTED);
        }
    }
}
