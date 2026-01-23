/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ohdsi.webapi.cohortdefinition;

import jakarta.persistence.*;
import org.ohdsi.analysis.Cohort;
import org.ohdsi.circe.cohortdefinition.CohortExpression;
import org.ohdsi.webapi.cohortanalysis.CohortAnalysisGenerationInfoEntity;
import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.model.TaggableEntity;
import org.ohdsi.webapi.tag.domain.Tag;

import java.io.Serial;
import java.util.*;

@Entity(name = "CohortDefinition")
@Table(name = "cohort_definition")
public class CohortDefinitionEntity extends TaggableEntity<Integer> implements Cohort {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cohort_definition_generator")
    @SequenceGenerator(name = "cohort_definition_generator", sequenceName = "cohort_definition_sequence", initialValue = 1, allocationSize = 1)
    @Access(AccessType.PROPERTY)
    private Integer id;

    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "expression_type")
    private ExpressionType expressionType;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false, orphanRemoval = true, mappedBy = "definition")
    @JoinColumn(name = "id")
    private CohortDefinitionDetails details;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "cohortDefinition")
    private Set<CohortGenerationInfo> generationInfoList;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "cohortDefinition")
    private Set<CohortAnalysisGenerationInfoEntity> cohortAnalysisGenerationInfoEntityList = new HashSet<>();

    @ManyToMany(targetEntity = CohortCharacterizationEntity.class, fetch = FetchType.LAZY)
    @JoinTable(name = "cc_cohort", joinColumns = @JoinColumn(name = "cohort_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "cohort_characterization_id", referencedColumnName = "id"))
    private List<CohortCharacterizationEntity> cohortCharacterizations = new ArrayList<>();

    @ManyToMany(targetEntity = Tag.class, fetch = FetchType.LAZY)
    @JoinTable(name = "cohort_tag", joinColumns = @JoinColumn(name = "asset_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "id"))
    private Set<Tag> tags;

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

    public CohortDefinitionEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public CohortDefinitionEntity setDescription(String description) {
        this.description = description;
        return this;
    }

    public ExpressionType getExpressionType() {
        return expressionType;
    }

    public CohortDefinitionEntity setExpressionType(ExpressionType expressionType) {
        this.expressionType = expressionType;
        return this;
    }

    public CohortDefinitionDetails getDetails() {
        return this.details;
    }

    public CohortDefinitionEntity setDetails(CohortDefinitionDetails details) {
        this.details = details;
        return this;
    }

    public Set<CohortGenerationInfo> getGenerationInfoList() {
        return this.generationInfoList;
    }

    public CohortDefinitionEntity setGenerationInfoList(Set<CohortGenerationInfo> list) {
        this.generationInfoList = list;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CohortDefinitionEntity that))
            return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    public Set<CohortAnalysisGenerationInfoEntity> getCohortAnalysisGenerationInfoList() {
        return cohortAnalysisGenerationInfoEntityList;
    }

    public void setCohortAnalysisGenerationInfoList(
            Set<CohortAnalysisGenerationInfoEntity> cohortAnalysisGenerationInfoEntityList) {
        this.cohortAnalysisGenerationInfoEntityList = cohortAnalysisGenerationInfoEntityList;
    }

    @Override
    public CohortExpression getExpression() {
        return details != null ? details.getExpressionObject() : null;
    }

    public List<CohortCharacterizationEntity> getCohortCharacterizations() {
        return cohortCharacterizations;
    }

    public void setCohortCharacterizations(final List<CohortCharacterizationEntity> cohortCharacterizations) {
        this.cohortCharacterizations = cohortCharacterizations;
    }

    @Override
    public Set<Tag> getTags() {
        return tags;
    }

    @Override
    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }
}
