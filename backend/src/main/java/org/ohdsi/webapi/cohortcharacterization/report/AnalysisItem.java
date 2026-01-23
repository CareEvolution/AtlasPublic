package org.ohdsi.webapi.cohortcharacterization.report;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.ohdsi.analysis.cohortcharacterization.design.CcResultType;
import org.ohdsi.webapi.cohortcharacterization.dto.CcResult;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;

import java.util.*;

public class AnalysisItem {
    // Key is covariate id and strata id
    private final Map<Pair<Long, Long>, List<CcResult>> map = new HashMap<>();
    private CcResultType type;
    private String name;
    private String faType;

    public List<CcResult> getOrCreateCovariateItem(Long covariateId, Long strataId) {
        Pair<Long, Long> key = new ImmutablePair<>(covariateId, strataId);
        map.putIfAbsent(key, new ArrayList<>());
        return map.get(key);
    }

    public void setType(CcResultType resultType) {
        this.type = resultType;
    }

    public CcResultType getType() {
        return this.type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFaType(String faType) {
        this.faType = faType;
    }

    public String getFaType() {
        return faType;
    }

    public AnalysisResultItem getSimpleItems(Map<Integer, CohortDefinitionEntity> definitionMap,
            Map<String, String> feAnalysisMap) {
        var values = new HashSet<ExportItem<?>>();
        var domainIds = new HashSet<String>();
        var cohorts = new HashSet<Cohort>();
        var factory = new ItemFactory();
        for (var results : map.values()) {
            for (var ccResult : results) {
                var cohortDef = definitionMap.get(ccResult.getCohortId());
                var item = factory.createItem(ccResult, cohortDef.getName());
                var domainId = feAnalysisMap.get(ccResult.getAnalysisName());
                item.setDomainId(domainId);
                domainIds.add(domainId);
                cohorts.add(new Cohort(cohortDef.getId(), cohortDef.getName()));
                values.add(item);
            }
        }
        return new AnalysisResultItem(domainIds, cohorts, values);
    }

    public AnalysisResultItem getComparativeItems(
            CohortDefinitionEntity firstCohortDef,
            CohortDefinitionEntity secondCohortDef,
            Map<String, String> feAnalysisMap) {
        var values = new HashSet<ExportItem<?>>();
        var domainIds = new HashSet<String>();
        var cohorts = new HashSet<Cohort>();
        cohorts.add(new Cohort(firstCohortDef.getId(), firstCohortDef.getName()));
        cohorts.add(new Cohort(secondCohortDef.getId(), secondCohortDef.getName()));
        var factory = new ItemFactory();
        for (var results : map.values()) {
            // create default items, because we can have result for only one cohort
            ExportItem<?> first = null;
            ExportItem<?> second = null;
            for (var ccResult : results) {
                if (Objects.equals(ccResult.getCohortId(), firstCohortDef.getId())) {
                    first = factory.createItem(ccResult, firstCohortDef.getName());
                } else {
                    second = factory.createItem(ccResult, secondCohortDef.getName());
                }
            }
            ExportItem<?> comparativeItem;
            if (first instanceof DistributionItem || second instanceof DistributionItem) {
                comparativeItem = new ComparativeDistributionItem((DistributionItem) first, (DistributionItem) second,
                        firstCohortDef, secondCohortDef);
            } else {
                comparativeItem = new ComparativeItem((PrevalenceItem<?>) first, (PrevalenceItem<?>) second,
                        firstCohortDef, secondCohortDef);
            }
            var domainId = feAnalysisMap.get(comparativeItem.getAnalysisName());
            comparativeItem.setDomainId(domainId);
            domainIds.add(domainId);
            values.add(comparativeItem);
        }
        return new AnalysisResultItem(domainIds, cohorts, values);
    }
}
