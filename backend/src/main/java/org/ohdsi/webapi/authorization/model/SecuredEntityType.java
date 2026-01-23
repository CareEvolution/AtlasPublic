package org.ohdsi.webapi.authorization.model;

import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.cohortsample.CohortSample;
import org.ohdsi.webapi.conceptset.ConceptSet;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisEntity;
import org.ohdsi.webapi.model.SecuredEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.tag.domain.Tag;

public enum SecuredEntityType {
    COHORT_DEFINITION(CohortDefinitionEntity.class),
    CONCEPT_SET(ConceptSet.class),
    COHORT_CHARACTERIZATION(CohortCharacterizationEntity.class),
    FE_ANALYSIS(FeAnalysisEntity.class),
    SOURCE(SourceEntity.class),
    COHORT_SAMPLE(CohortSample.class),
    TAG(Tag.class);

    private final Class<? extends SecuredEntity> entityClass;

    SecuredEntityType(Class<? extends SecuredEntity> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<? extends SecuredEntity> getEntityClass() {
        return entityClass;
    }

    public static SecuredEntityType fromEntityClass(Class<? extends SecuredEntity> entityClass) {
        for (SecuredEntityType type : values()) {
            if (type.getEntityClass().isAssignableFrom(entityClass)) {
                return type;
            }
        }

        return null;
    }
}
