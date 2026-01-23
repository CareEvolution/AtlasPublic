package org.ohdsi.webapi.authorization.permissionmapping;

import org.ohdsi.webapi.authorization.Permissions;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Service
public class UrlPermissionMapper {
    private final HashMap<String,String> urlPermissionMap = new HashMap<>();

    public UrlPermissionMapper() {

        // PERSON
        urlPermissionMap.put("*:person:*:get", Permissions.READ_PERSON);
        urlPermissionMap.put("*:person:*:get:dates", Permissions.READ_PERSON_DATES);

        // COHORT CHARACTERIZATION
        urlPermissionMap.put("cohort-characterization:*:delete", Permissions.DELETE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:design:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:download:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:exists:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:export:conceptset:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:export:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:generation:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:post", Permissions.UPDATE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:protectedtag:*:delete", Permissions.UPDATE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:protectedtag:post", Permissions.UPDATE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:put", Permissions.UPDATE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:tag:*:delete", Permissions.UPDATE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:tag:post", Permissions.UPDATE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:version:*:createAsset:put", Permissions.UPDATE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:version:*:delete", Permissions.UPDATE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:version:*:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:version:*:put", Permissions.UPDATE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:*:version:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:byTags:post", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:check:post", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:design:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:generation:*:delete", Permissions.UPDATE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:generation:*:design:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:generation:*:explore:prevalence:*:*:*:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:generation:*:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:generation:*:result:count:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:generation:*:result:export:post", Permissions.UPDATE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:generation:*:result:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:generation:*:result:post", Permissions.UPDATE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:get", Permissions.READ_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:import:post", Permissions.CREATE_COHORT_CHARACTERIZATION);
        urlPermissionMap.put("cohort-characterization:post", Permissions.CREATE_COHORT_CHARACTERIZATION);

        // COHORT ANALYSIS
        urlPermissionMap.put("cohortanalysis:*:get", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortanalysis:get", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortanalysis:post", Permissions.UPDATE_COHORT_DEFINITION);

        // COHORT DEFINITION
        urlPermissionMap.put("cohortdefinition:*:exists:get", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:get", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:get", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:info:get", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:post", Permissions.CREATE_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:sql:post", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:put", Permissions.UPDATE_COHORT_DEFINITION);

        urlPermissionMap.put("cohortdefinition:*:check:get", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:check:post", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:copy:get", Permissions.CREATE_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:delete", Permissions.DELETE_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:export:conceptset:get", Permissions.READ_COHORT_DEFINITION);

        urlPermissionMap.put("cohortdefinition:*:protectedtag:*:delete", Permissions.UPDATE_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:protectedtag:post", Permissions.UPDATE_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:tag:*:delete", Permissions.UPDATE_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:tag:post", Permissions.UPDATE_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:version:*:createAsset:put", Permissions.UPDATE_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:version:*:delete", Permissions.UPDATE_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:version:*:get", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:version:*:put", Permissions.UPDATE_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:*:version:get", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:byTags:post", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:check:post", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:checkv2:post", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:printfriendly:cohort:post", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortdefinition:printfriendly:conceptsets:post", Permissions.READ_COHORT_DEFINITION);

        // COHORT RESULTS
        urlPermissionMap.put("cohortresults:*:get", Permissions.READ_COHORT_DEFINITION);

        // COHORT SAMPLE
        urlPermissionMap.put("cohortsample:*:*:*:delete", Permissions.UPDATE_COHORT_DEFINITION);
        urlPermissionMap.put("cohortsample:*:*:*:get", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortsample:*:*:*:refresh:post", Permissions.UPDATE_COHORT_DEFINITION);
        urlPermissionMap.put("cohortsample:*:*:delete", Permissions.UPDATE_COHORT_DEFINITION);
        urlPermissionMap.put("cohortsample:*:*:get", Permissions.READ_COHORT_DEFINITION);
        urlPermissionMap.put("cohortsample:*:*:post", Permissions.UPDATE_COHORT_DEFINITION);

        // CONCEPT SET
        urlPermissionMap.put("conceptset:*:copy-name:get", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:delete", Permissions.DELETE_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:exists:get", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:export:get", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:expression:*:get", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:expression:get", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:generationinfo:get", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:get", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:items:put", Permissions.UPDATE_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:protectedtag:*:delete", Permissions.UPDATE_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:protectedtag:post", Permissions.UPDATE_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:put", Permissions.UPDATE_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:tag:*:delete", Permissions.UPDATE_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:tag:post", Permissions.UPDATE_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:version:*:createAsset:put", Permissions.UPDATE_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:version:*:delete", Permissions.UPDATE_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:version:*:expression:*:get", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:version:*:expression:get", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:version:*:get", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:version:*:put", Permissions.UPDATE_CONCEPT_SET);
        urlPermissionMap.put("conceptset:*:version:get", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:byTags:post", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:check:post", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:get", Permissions.READ_CONCEPT_SET);
        urlPermissionMap.put("conceptset:post", Permissions.CREATE_CONCEPT_SET);

        // FEATURE ANALYSIS
        urlPermissionMap.put("feature-analysis:*:copy:get", Permissions.CREATE_FEATURE_ANALYSIS);
        urlPermissionMap.put("feature-analysis:*:exists:get", Permissions.READ_FEATURE_ANALYSIS);
        urlPermissionMap.put("feature-analysis:*:export:conceptset:get", Permissions.READ_FEATURE_ANALYSIS);
        urlPermissionMap.put("feature-analysis:*:get", Permissions.READ_FEATURE_ANALYSIS);
        urlPermissionMap.put("feature-analysis:aggregates:get", Permissions.READ_FEATURE_ANALYSIS);
        urlPermissionMap.put("feature-analysis:get", Permissions.READ_FEATURE_ANALYSIS);
        urlPermissionMap.put("feature-analysis:post", Permissions.CREATE_FEATURE_ANALYSIS);

        // JOB
        urlPermissionMap.put("job:execution:get", Permissions.READ_JOB);
        urlPermissionMap.put("job:get", Permissions.READ_JOB);
        urlPermissionMap.put("job:type:*:name:*:get", Permissions.READ_JOB);

        // NOTIFICATIONS
        urlPermissionMap.put("notifications:get", Permissions.READ_NOTIFICATIONS);
        urlPermissionMap.put("notifications:viewed:get", Permissions.READ_NOTIFICATIONS);
        urlPermissionMap.put("notifications:viewed:post", Permissions.UPDATE_NOTIFICATIONS);

        // PERMISSION
        urlPermissionMap.put("permission:get", Permissions.READ_PERMISSION);

        // ROLE
        urlPermissionMap.put("role:*:get", Permissions.READ_ROLE);
        urlPermissionMap.put("role:*:permissions:get", Permissions.READ_ROLE + "+" + Permissions.READ_PERMISSION);
        urlPermissionMap.put("role:*:users:*:delete", Permissions.READ_ROLE + "+" + Permissions.UPDATE_USER);
        urlPermissionMap.put("role:*:users:*:put", Permissions.UPDATE_ROLE + "+" + Permissions.READ_USER);
        urlPermissionMap.put("role:*:users:get", Permissions.READ_ROLE + "+" + Permissions.READ_USER);
        urlPermissionMap.put("role:1:permissions:*:delete", Permissions.UPDATE_ROLE);
        urlPermissionMap.put("role:1:permissions:*:put", Permissions.UPDATE_ROLE + "+" + Permissions.READ_PERMISSION);
        urlPermissionMap.put("role:get", Permissions.READ_ROLE);
        urlPermissionMap.put("role:post", Permissions.CREATE_ROLE);

        // SOURCE
        urlPermissionMap.put("source:*:daimons:*:set-priority:post", Permissions.UPDATE_SOURCE);
        urlPermissionMap.put("source:connection:*:get", Permissions.READ_SOURCE);
        urlPermissionMap.put("source:daimon:priority:get", Permissions.READ_SOURCE);
        urlPermissionMap.put("source:details:*:get", Permissions.READ_SOURCE);
        urlPermissionMap.put("source:priorityVocabulary:get", Permissions.READ_SOURCE);
        urlPermissionMap.put("source:*:delete", Permissions.DELETE_SOURCE);
        urlPermissionMap.put("source:*:get", Permissions.READ_SOURCE);
        urlPermissionMap.put("source:*:put", Permissions.UPDATE_SOURCE);
        urlPermissionMap.put("source:post", Permissions.CREATE_SOURCE);

        // TAG
        urlPermissionMap.put("tag:*:delete", Permissions.DELETE_TAG);
        urlPermissionMap.put("tag:*:get", Permissions.READ_TAG);
        urlPermissionMap.put("tag:*:put", Permissions.UPDATE_TAG);
        urlPermissionMap.put("tag:get", Permissions.READ_TAG);
        urlPermissionMap.put("tag:multiAssign:post", "");
        urlPermissionMap.put("tag:multiUnassign:post", "");
        urlPermissionMap.put("tag:post", Permissions.CREATE_TAG);
        urlPermissionMap.put("tag:search:get", Permissions.READ_TAG);

        // USER
        urlPermissionMap.put("user:get", Permissions.READ_USER);

        // VOCABULARY
        urlPermissionMap.put("vocabulary:*:compare-arbitrary", "");
        urlPermissionMap.put("vocabulary:*:post", "");
    }

    public List<String> getMappedPermission(String oldPermission) {
        var newPermissions = new ArrayList<String>();

        // Handle special cases that come from things like the source roles.
        if (oldPermission.startsWith("cohortdefinition:*:cancel:")) {
            // Example: cohortdefinition:*:cancel:EUNOMIA:get
            newPermissions.add(Permissions.READ_COHORT_DEFINITION);
            newPermissions.add(Permissions.READ_SOURCE);
            return newPermissions;
        }

        if (oldPermission.startsWith("cohortdefinition:*:generate:")) {
            // Example: cohortdefinition:*:generate:EUNOMIA:get
            newPermissions.add(Permissions.READ_COHORT_DEFINITION);
            newPermissions.add(Permissions.READ_SOURCE);
            return newPermissions;
        }

        var mappedPermission = urlPermissionMap.getOrDefault(oldPermission, null);
        if (StringUtils.hasText(mappedPermission)) {
            // Support + notation, the old-new map may need to return multiples
            if (mappedPermission.contains("+")) {
                newPermissions.addAll(Arrays.asList(mappedPermission.split("\\+")));
            } else {
                newPermissions.add(mappedPermission);
            }
        }
        return newPermissions;
    }
}
