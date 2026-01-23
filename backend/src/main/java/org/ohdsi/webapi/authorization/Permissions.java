package org.ohdsi.webapi.authorization;

import org.springframework.stereotype.Component;

@Component("PERMS")
public class Permissions {
    public static final String CREATE_COHORT_DEFINITION = "cohortdefinition:create";
    public static final String DELETE_COHORT_DEFINITION = "cohortdefinition:delete";
    public static final String READ_COHORT_DEFINITION = "cohortdefinition:read";
    public static final String UPDATE_COHORT_DEFINITION = "cohortdefinition:update";

    public static final String CREATE_SOURCE = "source:create";
    public static final String DELETE_SOURCE = "source:delete";
    public static final String READ_SOURCE = "source:read";
    public static final String UPDATE_SOURCE = "source:update";

    public static final String CREATE_PERMISSION = "permission:create";
    public static final String DELETE_PERMISSION = "permission:delete";
    public static final String READ_PERMISSION = "permission:read";
    public static final String UPDATE_PERMISSION = "permission:update";

    public static final String CREATE_ROLE = "role:create";
    public static final String DELETE_ROLE = "role:delete";
    public static final String READ_ROLE = "role:read";
    public static final String UPDATE_ROLE = "role:update";

    public static final String CREATE_TAG = "tag:create";
    public static final String READ_TAG = "tag:read";
    public static final String UPDATE_TAG = "tag:update";
    public static final String DELETE_TAG = "tag:delete";

    public static final String READ_USER = "user:read";
    public static final String UPDATE_USER = "user:update";

    public static final String READ_JOB = "job:read";
    public static final String READ_NOTIFICATIONS = "notifications:read";
    public static final String UPDATE_NOTIFICATIONS = "notifications:update";

    public static final String READ_PERSON = "person:read";
    public static final String READ_PERSON_DATES = "person:read-dates";

    public static final String READ_CONCEPT_SET = "conceptset:read";
    public static final String UPDATE_CONCEPT_SET = "conceptset:update";
    public static final String DELETE_CONCEPT_SET = "conceptset:delete";
    public static final String CREATE_CONCEPT_SET = "conceptset:create";

    public static final String CREATE_COHORT_CHARACTERIZATION = "cohort-characterization:create";
    public static final String DELETE_COHORT_CHARACTERIZATION = "cohort-characterization:delete";
    public static final String READ_COHORT_CHARACTERIZATION = "cohort-characterization:read";
    public static final String UPDATE_COHORT_CHARACTERIZATION = "cohort-characterization:update";

    public static final String CREATE_FEATURE_ANALYSIS = "feature-analysis:create";
    public static final String DELETE_FEATURE_ANALYSIS = "feature-analysis:delete";
    public static final String READ_FEATURE_ANALYSIS = "feature-analysis:read";
    public static final String UPDATE_FEATURE_ANALYSIS = "feature-analysis:update";

    public static final String READ_SENSITIVE_INFO = "sensitive-info:read";
}
