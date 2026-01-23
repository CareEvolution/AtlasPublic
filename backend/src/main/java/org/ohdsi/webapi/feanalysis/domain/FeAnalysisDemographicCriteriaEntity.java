package org.ohdsi.webapi.feanalysis.domain;

import org.ohdsi.analysis.Utils;
import org.ohdsi.analysis.cohortcharacterization.design.DemographicCriteriaFeature;
import org.ohdsi.circe.cohortdefinition.DemographicCriteria;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("DEMOGRAPHIC_CRITERIA")
public class FeAnalysisDemographicCriteriaEntity extends FeAnalysisDistributionCriteriaEntity<DemographicCriteria>
        implements DemographicCriteriaFeature {

    // TODO: Warning - Basic attribute type should not be DemographicCriteria
    //       (which is defined in circe:CohortDefinition)
    //       same error is in backend
    @Override
    public DemographicCriteria getExpression() {
        return Utils.deserialize(this.getExpressionString(), DemographicCriteria.class);
    }
}
