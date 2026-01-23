package org.ohdsi.webapi.feanalysis.domain;

import org.ohdsi.webapi.common.domain.CommonConceptSetEntity;

import jakarta.persistence.*;

@Entity
@Table(name = "fe_analysis_conceptset")
public class FeAnalysisConcepsetEntity extends CommonConceptSetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fe_conceptset_generator")
    @SequenceGenerator(name = "fe_conceptset_generator", sequenceName = "fe_conceptset_sequence", initialValue = 1, allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fe_analysis_id")
    private FeAnalysisWithCriteriaEntity featureAnalysis;

    public FeAnalysisConcepsetEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FeAnalysisWithCriteriaEntity getFeatureAnalysis() {
        return featureAnalysis;
    }

    public void setFeatureAnalysis(FeAnalysisWithCriteriaEntity featureAnalysis) {
        this.featureAnalysis = featureAnalysis;
    }
}
