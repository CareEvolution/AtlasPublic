package org.ohdsi.webapi.feanalysis.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.DiscriminatorOptions;
import org.ohdsi.analysis.WithId;

// TODO a lot of "raw use of parameterized class" warnings
// TODO also the structure feels odd: FeAnalysisWithCriteriaEntity extends
//      THIS class, and is then used in this class.
@Entity
@Table(name = "fe_analysis_criteria")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "criteria_type")
@DiscriminatorOptions(force = false)
public abstract class FeAnalysisCriteriaEntity implements WithId<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fe_analysis_criteria_generator")
    @SequenceGenerator(name = "fe_analysis_criteria_generator", sequenceName = "fe_analysis_criteria_sequence", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column
    private String name;

    @Column(name = "expression", columnDefinition = "CLOB")
    private String expressionString;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fe_aggregate_id")
    private FeAnalysisAggregateEntity aggregate;

    @ManyToOne(optional = false, targetEntity = FeAnalysisWithCriteriaEntity.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "fe_analysis_id")
    private FeAnalysisWithCriteriaEntity featureAnalysis;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public FeAnalysisWithCriteriaEntity getFeatureAnalysis() {
        return featureAnalysis;
    }

    public void setFeatureAnalysis(final FeAnalysisWithCriteriaEntity featureAnalysis) {
        this.featureAnalysis = featureAnalysis;
    }

    public String getExpressionString() {
        return expressionString;
    }

    public void setExpressionString(final String expressionString) {
        this.expressionString = expressionString;
    }

    public FeAnalysisAggregateEntity getAggregate() {
        return aggregate;
    }

    public void setAggregate(FeAnalysisAggregateEntity aggregate) {
        this.aggregate = aggregate;
    }
}
