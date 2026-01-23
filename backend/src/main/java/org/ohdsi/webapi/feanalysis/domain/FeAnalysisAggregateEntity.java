package org.ohdsi.webapi.feanalysis.domain;

import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.analysis.TableJoin;
import org.ohdsi.analysis.cohortcharacterization.design.AggregateFunction;
import org.ohdsi.analysis.cohortcharacterization.design.FeatureAnalysisAggregate;
import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisDomain;
import org.ohdsi.circe.cohortdefinition.builders.CriteriaColumn;

import java.util.List;

@Entity
@Table(name = "fe_analysis_aggregate")
public class FeAnalysisAggregateEntity implements FeatureAnalysisAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fe_aggregate_generator")
    @SequenceGenerator(name = "fe_aggregate_generator", sequenceName = "fe_aggregate_sequence", initialValue = 1, allocationSize = 1)
    private Integer id;

    @Column
    private String name;

    @Column
    @Enumerated(value = EnumType.STRING)
    private StandardFeatureAnalysisDomain domain;

    @Column(name = "agg_function")
    @Enumerated(value = EnumType.STRING)
    private AggregateFunction function;

    @Column
    private String expression;

    @Column(name = "join_table")
    private String joinTable;

    @Column(name = "join_type")
    @Enumerated(EnumType.STRING)
    private TableJoin joinType;

    @Column(name = "join_condition")
    private String joinCondition;

    @Column(name = "is_default")
    private boolean isDefault;

    @Column(name = "missing_means_zero")
    private boolean isMissingMeansZero;

    @Column(name = "criteria_columns", columnDefinition = "VARCHAR")
    @Convert(converter = CriteriaColumnListConverter.class)
    private List<CriteriaColumn> columns;

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StandardFeatureAnalysisDomain getDomain() {
        return domain;
    }

    public void setDomain(StandardFeatureAnalysisDomain domain) {
        this.domain = domain;
    }

    public AggregateFunction getFunction() {
        return function;
    }

    @Override
    public List<CriteriaColumn> getAdditionalColumns() {
        return columns;
    }

    public void setCriteriaColumns(List<CriteriaColumn> columns) {
        this.columns = columns;
    }

    public void setFunction(AggregateFunction function) {
        this.function = function;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public boolean hasQuery() {
        return StringUtils.isNotBlank(this.joinTable);
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getJoinTable() {
        return joinTable;
    }

    public void setJoinTable(String joinTable) {
        this.joinTable = joinTable;
    }

    public TableJoin getJoinType() {
        return joinType;
    }

    public void setJoinType(TableJoin joinType) {
        this.joinType = joinType;
    }

    public String getJoinCondition() {
        return joinCondition;
    }

    public void setJoinCondition(String joinCondition) {
        this.joinCondition = joinCondition;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isMissingMeansZero() {
        return isMissingMeansZero;
    }

    public void setMissingMeansZero(boolean aDefault) {
        isMissingMeansZero = aDefault;
    }
}
