package org.ohdsi.webapi.feanalysis.domain;

import org.ohdsi.analysis.Utils;
import org.ohdsi.analysis.cohortcharacterization.design.CriteriaFeature;
import org.ohdsi.circe.cohortdefinition.CriteriaGroup;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CRITERIA_GROUP")
public class FeAnalysisCriteriaGroupEntity extends FeAnalysisCriteriaEntity implements CriteriaFeature {

    // TODO: warning - basic attribute type should not be CriteriaGroup (which
    //       is defined in circe.CohortDefinition).
    @Override
    public CriteriaGroup getExpression() {
        return getCriteriaGroup();
    }

    private CriteriaGroup getCriteriaGroup() {
        return Utils.deserialize(this.getExpressionString(), CriteriaGroup.class);
    }
}
