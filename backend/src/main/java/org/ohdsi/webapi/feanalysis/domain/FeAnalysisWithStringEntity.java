package org.ohdsi.webapi.feanalysis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("not null")
public class FeAnalysisWithStringEntity extends FeAnalysisEntity<String> {
    public FeAnalysisWithStringEntity() {
        super();
    }

    public FeAnalysisWithStringEntity(final FeAnalysisWithStringEntity analysis) {
        super(analysis);
    }

    @Column(columnDefinition = "CLOB")
    private String design;

    @Override
    public String getDesign() {
        return design;
    }

    public void setDesign(final String design) {
        this.design = design;
    }
}
