package org.ohdsi.webapi.cohortsample;

import jakarta.persistence.*;
import org.ohdsi.webapi.model.SecuredEntity;

import java.util.List;

@Entity(name = "CohortSample")
@Table(name = "cohort_sample")
public class CohortSample extends SecuredEntity<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cohort_sample_generator")
    @SequenceGenerator(name = "cohort_sample_generator", sequenceName = "cohort_sample_sequence", initialValue = 1, allocationSize = 1)
    private Integer id;

    @Column
    private String name;

    @Column(name = "cohort_definition_id")
    private int cohortDefinitionId;

    @Column(name = "source_id")
    private int sourceId;

    @Column(name = "age_mode")
    private String ageMode;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Column(name = "gender_concept_ids")
    private String genderConceptIds;

    @Column(name = "\"size\"")
    private int size;

    @Transient
    private List<SampleElement> elements;

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getCohortDefinitionId() {
        return cohortDefinitionId;
    }

    public void setCohortDefinitionId(int cohortDefinitionId) {
        this.cohortDefinitionId = cohortDefinitionId;
    }

    public List<SampleElement> getElements() {
        return elements;
    }

    public void setElements(List<SampleElement> elements) {
        this.elements = elements;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Integer getAgeMin() {
        return ageMin;
    }

    public void setAgeMin(Integer ageMin) {
        this.ageMin = ageMin;
    }

    public Integer getAgeMax() {
        return ageMax;
    }

    public void setAgeMax(Integer ageMax) {
        this.ageMax = ageMax;
    }

    public String getGenderConceptIds() {
        return genderConceptIds;
    }

    public void setGenderConceptIds(String genderConceptIds) {
        this.genderConceptIds = genderConceptIds;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getAgeMode() {
        return ageMode;
    }

    public void setAgeMode(String ageMode) {
        this.ageMode = ageMode;
    }
}
