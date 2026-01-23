package org.ohdsi.webapi.common.domain;

import org.ohdsi.analysis.Utils;
import org.ohdsi.circe.cohortdefinition.ConceptSet;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.List;
import java.util.Objects;

@MappedSuperclass
public class CommonConceptSetEntity {
    @Column(name = "expression", columnDefinition = "CLOB")
    private String rawExpression;

    public String getRawExpression() {
        return rawExpression;
    }

    public void setRawExpression(String rawExpression) {
        this.rawExpression = rawExpression;
    }

    public List<ConceptSet> getConceptSets() {
        return Objects.nonNull(this.rawExpression) ?
                Utils.deserialize(this.rawExpression,
                        typeFactory -> typeFactory.constructCollectionType(List.class, ConceptSet.class)) : null;
    }
}
