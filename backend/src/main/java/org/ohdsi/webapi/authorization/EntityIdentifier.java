package org.ohdsi.webapi.authorization;

import org.ohdsi.webapi.authorization.model.SecuredEntityType;
import org.ohdsi.webapi.model.SecuredEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

import static java.util.Map.entry;

@Service
public class EntityIdentifier {
    private final Map<SecuredEntityType, String> securedEntityTypeToResourceNameMap;

    public EntityIdentifier() {
        securedEntityTypeToResourceNameMap = Map.ofEntries(
                entry(SecuredEntityType.COHORT_CHARACTERIZATION, "cohort-characterization"),
                entry(SecuredEntityType.COHORT_DEFINITION, "cohortdefinition"),
                entry(SecuredEntityType.COHORT_SAMPLE, "cohortsample"),
                entry(SecuredEntityType.CONCEPT_SET, "conceptset"),
                entry(SecuredEntityType.FE_ANALYSIS, "feature-analysis"),
                entry(SecuredEntityType.SOURCE, "source"),
                entry(SecuredEntityType.TAG, "tag")
        );
    }

    public SecuredEntityType getSecuredEntityType(SecuredEntity<?> entity) {
        var securedEntityType = SecuredEntityType.fromEntityClass(entity.getClass());
        if (securedEntityType == null) {
            throw new RuntimeException("Cannot determine entity type for " + entity.getClass().getName());
        }
        return securedEntityType;
    }

    public String getEntityName(SecuredEntityType securedEntityType) {
        var resource = securedEntityTypeToResourceNameMap.get(securedEntityType);
        if (resource == null) {
            throw new RuntimeException("Cannot determine resource name for " + securedEntityType.name());
        }
        return resource;
    }
}
