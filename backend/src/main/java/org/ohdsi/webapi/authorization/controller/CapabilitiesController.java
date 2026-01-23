package org.ohdsi.webapi.authorization.controller;

import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.Permissions;
import org.ohdsi.webapi.cdmresults.action.CacheActions;
import org.ohdsi.webapi.source.SourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for checking user capabilities. This controller provides endpoints to check if the current 
 * user has permissions to perform certain actions.
 */
@RestController
@RequestMapping("/capabilities")
public class CapabilitiesController {

    private final AuthorizationManager authorizationManager;
    private final CacheActions cacheActions;
    private final SourceService sourceService;

    public CapabilitiesController(AuthorizationManager authorizationManager, CacheActions cacheActions, SourceService sourceService) {
        this.authorizationManager = authorizationManager;
        this.cacheActions = cacheActions;
        this.sourceService = sourceService;
    }

    /**
     * Check if the current user has permission to create a new source.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/createSource")
    public Boolean canCreateSource() {
        return authorizationManager.currentUserHasPermission(Permissions.CREATE_SOURCE);
    }

    /**
     * Check if the current user has permission to create a new tag.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/createTag")
    public Boolean canCreateTag() {
        return authorizationManager.currentUserHasPermission(Permissions.CREATE_TAG);
    }

    /**
     * Check if the current user has permission to create a new cohort definition.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/createCohortDefinition")
    public Boolean canCreateCohortDefinition() {
        return authorizationManager.currentUserHasPermission(Permissions.CREATE_COHORT_DEFINITION);
    }

    /**
     * Check if the current user has permission to clear the sources cache of all sources.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/clearAllSourcesCache")
    public Boolean canClearAllSourcesCache() {
        if (!authorizationManager.currentUserHasPermission(Permissions.UPDATE_SOURCE)) {
            return false;
        }
        var sources = cacheActions.getAllSources();
        if (!sources.stream().allMatch(source -> authorizationManager.currentUserCanUpdate(source))) {
            return false;
        }
        return true;
    }

    /**
     * Check if the current user has permission to search the priority vocabulary.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/searchVocabulary")
    public Boolean canSearchVocabulary() {
        var source = sourceService.getPriorityVocabularySource();
        return source != null && authorizationManager.currentUserHasPermission(Permissions.READ_SOURCE);
    }

    /**
     * Check if the current user has permission to view persons.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/readPerson")
    public Boolean canReadPerson() {
        return authorizationManager.currentUserHasPermission(Permissions.READ_PERSON);
    }

    /**
     * Check if the current user has permission to view person dates.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/readPersonDates")
    public Boolean canReadPersonDates() {
        return authorizationManager.currentUserHasPermission(Permissions.READ_PERSON_DATES);
    }

    /**
     * Check if the current user has permission to view notifications.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/readNotifications")
    public Boolean canReadNotifications() {
        return authorizationManager.currentUserHasPermission(Permissions.READ_NOTIFICATIONS);
    }

    /**
     * Check if the current user has permission to update notifications.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/updateNotifications")
    public Boolean canUpdateNotifications() {
        return authorizationManager.currentUserHasPermission(Permissions.UPDATE_NOTIFICATIONS);
    }

    /**
     * Check if the current user has permission to view roles.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/readRoles")
    public Boolean canReadRoles() {
        return authorizationManager.currentUserHasPermission(Permissions.READ_ROLE);
    }

    /**
     * Check if the current user has permission to create a new role.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/createRole")
    public Boolean canCreateRole() {
        return authorizationManager.currentUserHasPermission(Permissions.CREATE_ROLE);
    }

    /**
     * Check if the current user has permission to update roles.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/updateRole")
    public Boolean canUpdateRole() {
        return authorizationManager.currentUserHasPermission(Permissions.UPDATE_ROLE);
    }

    /**
     * Check if the current user has permission to delete roles.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/deleteRole")
    public Boolean canDeleteRole() {
        return authorizationManager.currentUserHasPermission(Permissions.DELETE_ROLE);
    }

    /**
     * Check if the current user has permission to create a new concept set.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/createConceptSet")
    public Boolean canCreateConceptSet() {
        return authorizationManager.currentUserHasPermission(Permissions.CREATE_CONCEPT_SET);
    }

    /**
     * Check if the current user has permission to create a new cohort characterization.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/createCohortCharacterization")
    public Boolean canCreateCohortCharacterization() {
        return authorizationManager.currentUserHasPermission(Permissions.CREATE_COHORT_CHARACTERIZATION);
    }

    /**
     * Check if the current user has permission to create a new feature analysis.
     * @return True if the user has permission, false otherwise.
     */
    @GetMapping("/createFeatureAnalysis")
    public Boolean canCreateFeatureAnalysis() {
        return authorizationManager.currentUserHasPermission(Permissions.CREATE_FEATURE_ANALYSIS);
    }
}
