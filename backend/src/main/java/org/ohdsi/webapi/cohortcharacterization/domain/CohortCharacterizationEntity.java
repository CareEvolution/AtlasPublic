package org.ohdsi.webapi.cohortcharacterization.domain;

import jakarta.persistence.*;
import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterization;
import org.ohdsi.circe.cohortdefinition.ConceptSet;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisEntity;
import org.ohdsi.webapi.model.TaggableEntity;
import org.ohdsi.webapi.tag.domain.Tag;

import java.util.*;

@Entity
@Table(name = "cohort_characterization")
public class CohortCharacterizationEntity extends TaggableEntity<Long> implements CohortCharacterization {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cohort_characterization_generator")
    @SequenceGenerator(name = "cohort_characterization_generator", sequenceName = "cohort_characterization_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @Column
    private String name;

    @Column
    private String description;

    @ManyToMany(targetEntity = CohortDefinitionEntity.class, fetch = FetchType.LAZY)
    @JoinTable(name = "cc_cohort", joinColumns = @JoinColumn(name = "cohort_characterization_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "cohort_id", referencedColumnName = "id"))
    private Set<CohortDefinitionEntity> cohortDefinitions = new HashSet<>();

    @ManyToMany(targetEntity = FeAnalysisEntity.class, fetch = FetchType.LAZY)
    @JoinTable(name = "cc_analysis", joinColumns = @JoinColumn(name = "cohort_characterization_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "fe_analysis_id", referencedColumnName = "id"))
    private Set<FeAnalysisEntity> featureAnalyses = new HashSet<>();

    @OneToMany(mappedBy = "cohortCharacterization", fetch = FetchType.LAZY, targetEntity = CcParamEntity.class)
    private Set<CcParamEntity> parameters = new HashSet<>();

    @OneToMany(mappedBy = "cohortCharacterization", fetch = FetchType.LAZY, targetEntity = CcStrataEntity.class)
    private Set<CcStrataEntity> stratas = new HashSet<>();

    @Column(name = "stratified_by")
    private String stratifiedBy;

    @Column(name = "strata_only")
    private Boolean strataOnly;

    @OneToOne(mappedBy = "cohortCharacterization", cascade = CascadeType.ALL)
    private CcStrataConceptSetEntity conceptSetEntity;

    @Column(name = "hash_code")
    private Integer hashCode;

    @ManyToMany(targetEntity = Tag.class, fetch = FetchType.LAZY)
    @JoinTable(name = "cohort_characterization_tag", joinColumns = @JoinColumn(name = "asset_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "id"))
    private Set<Tag> tags;

    @Override
    public Set<CohortDefinitionEntity> getCohorts() {
        return cohortDefinitions;
    }

    @Override
    public Set<FeAnalysisEntity> getFeatureAnalyses() {
        return featureAnalyses;
    }

    @Override
    public Set<CcParamEntity> getParameters() {
        return parameters;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setParameters(final Set<CcParamEntity> parameters) {
        this.parameters = parameters;
    }

    public void setFeatureAnalyses(final Set<FeAnalysisEntity> featureAnalyses) {
        this.featureAnalyses = featureAnalyses;
    }

    public Set<CohortDefinitionEntity> getCohortDefinitions() {
        return cohortDefinitions;
    }

    public void setCohortDefinitions(final Set<CohortDefinitionEntity> cohortDefinitions) {
        this.cohortDefinitions = cohortDefinitions;
    }

    @Override
    public Set<CcStrataEntity> getStratas() {
        return stratas;
    }

    public void setStratas(Set<CcStrataEntity> stratas) {
        this.stratas = stratas;
    }

    public String getStratifiedBy() {
        return stratifiedBy;
    }

    public void setStratifiedBy(String stratifiedBy) {
        this.stratifiedBy = stratifiedBy;
    }

    public Boolean getStrataOnly() {
        return Objects.nonNull(strataOnly) ? strataOnly : false;
    }

    public void setStrataOnly(Boolean strataOnly) {
        this.strataOnly = strataOnly;
    }

    public CcStrataConceptSetEntity getConceptSetEntity() {
        return conceptSetEntity;
    }

    public void setConceptSetEntity(CcStrataConceptSetEntity conceptSetEntity) {
        this.conceptSetEntity = conceptSetEntity;
    }

    @Override
    public Collection<ConceptSet> getStrataConceptSets() {
        return Objects.nonNull(this.conceptSetEntity) ? this.conceptSetEntity.getConceptSets()
                : Collections.emptyList();
    }

    public Integer getHashCode() {
        return hashCode;
    }

    public void setHashCode(final Integer hashCode) {
        this.hashCode = hashCode;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }
}
