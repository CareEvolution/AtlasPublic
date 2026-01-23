package org.ohdsi.webapi.feanalysis.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

// TODO raw use of parameterized class FeAnalysisDistributionCriteriaEntity
@Entity
@DiscriminatorValue("CRITERIA_SET_DISTRIBUTION")
public class FeAnalysisWithDistributionCriteriaEntity
        extends FeAnalysisWithCriteriaEntity<FeAnalysisDistributionCriteriaEntity> {

    public FeAnalysisWithDistributionCriteriaEntity() {
    }

    public FeAnalysisWithDistributionCriteriaEntity(FeAnalysisWithCriteriaEntity analysis) {
        super(analysis);
    }
}
