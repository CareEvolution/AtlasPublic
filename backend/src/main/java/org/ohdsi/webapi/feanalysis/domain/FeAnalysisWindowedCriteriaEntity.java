package org.ohdsi.webapi.feanalysis.domain;

import org.ohdsi.analysis.Utils;
import org.ohdsi.analysis.cohortcharacterization.design.WindowedCriteriaFeature;
import org.ohdsi.circe.cohortdefinition.WindowedCriteria;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("WINDOWED_CRITERIA")
public class FeAnalysisWindowedCriteriaEntity extends FeAnalysisDistributionCriteriaEntity<WindowedCriteria>
        implements WindowedCriteriaFeature {

    // TODO Basic attribute type should not be WindowedCriteria
    @Override
    public WindowedCriteria getExpression() {
        return Utils.deserialize(this.getExpressionString(), WindowedCriteria.class);
    }

}
