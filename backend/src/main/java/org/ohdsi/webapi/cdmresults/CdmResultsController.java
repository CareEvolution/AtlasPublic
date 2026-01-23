package org.ohdsi.webapi.cdmresults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.ohdsi.webapi.achilles.aspect.AchillesCache;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.cdmresults.action.CacheActions;
import org.ohdsi.webapi.cdmresults.action.ConceptActions;
import org.ohdsi.webapi.cdmresults.action.DomainReportActions;
import org.ohdsi.webapi.cdmresults.service.CDMCacheService;
import org.ohdsi.webapi.job.JobExecutionResource;
import org.ohdsi.webapi.report.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;

import java.util.AbstractMap;
import java.util.List;

/**
 * REST API for retrieving data source reports, and for managing the reports cache.
 */
@RestController
@RequestMapping("/cdmresults")
@RequestScope
public class CdmResultsController {
    private final CacheActions cacheActions;
    private final ConceptActions conceptActions;
    private final DomainReportActions domainReportActions;
    private final AuthorizationManager authorizationManager;

    public CdmResultsController(
            CDMCacheService cdmCacheService,
            CacheActions cacheActions,
            ConceptActions conceptActions,
            DomainReportActions domainReportActions,
            AuthorizationManager authorizationManager) {
        this.cacheActions = cacheActions;
        this.conceptActions = conceptActions;
        this.domainReportActions = domainReportActions;
        this.authorizationManager = authorizationManager;
    }

    // ===========================================================================================
    // Cache endpoints
    // ===========================================================================================

    /**
     * Clear the cdm_cache and achilles_cache for all sources
     *
     * @throws AccessDeniedException
     *         if the user is not an admin
     */
    @PostMapping("clearCache")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_SOURCE)")
    public void clearCache() {
        var sources = cacheActions.getAllSources();
        if (!sources.stream().allMatch(source -> authorizationManager.currentUserCanUpdate(source))) {
            throw new AccessDeniedException("User does not have permission to update one or more sources");
        }
        cacheActions.clearAllCaches();
    }

    /**
     * Refresh the results cache for a data source
     *
     * @param sourceKey
     *         The key for the data source
     * @return The job execution resource
     */
    @GetMapping("{sourceKey}/refreshCache")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_SOURCE)")
    public JobExecutionResource refreshCache(@PathVariable("sourceKey") final String sourceKey) {
        var source = cacheActions.getSource(sourceKey);
        if (!authorizationManager.currentUserCanUpdate(source)) {
            throw new AccessDeniedException("User does not have permission to update this source");
        }
        return cacheActions.refreshCache(sourceKey);
    }

    /**
     * Clear the cdm_cache and achilles_cache for a data source
     *
     * @param sourceKey
     *         The key for the data source
     * @throws AccessDeniedException
     *         if the user is not an admin
     */
    @PostMapping("{sourceKey}/clearCache")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_SOURCE)")
    public void clearCacheForSource(@PathVariable("sourceKey") final String sourceKey) {
        var source = cacheActions.getSource(sourceKey);
        if (!authorizationManager.currentUserCanUpdate(source)) {
            throw new AccessDeniedException("User does not have permission to update this source");
        }
        cacheActions.clearCacheForSource(sourceKey);
    }

    // ===========================================================================================
    // Concept endpoints
    // ===========================================================================================

    /**
     * Get the record count and descendant record count for one or more concepts in a data source
     *
     * @param sourceKey
     *         The key for the data source to query
     * @param identifiers
     *         A JSON array of concept identifiers to query (e.g. [201826, 437827])
     * @return A JSON object with one element per concept. Each element is an array of lenth two containing the
     *         record count and descendent record count for the concept.
     *
     *         <p>
     *         [ { "201826": [ 612861, 653173 ] }, { "437827": [ 224421, 224421 ] } ]
     *         </p>
     *         For concept id "201826" in the SYNPUF5PCT data source the record count is 612861 and the descendant
     *         record count is 653173.
     */
    @PostMapping("{sourceKey}/conceptRecordCount")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<AbstractMap.SimpleEntry<Integer, List<Long>>> getConceptRecordCount(@PathVariable("sourceKey") String sourceKey,
            @RequestBody() List<Integer> identifiers) {
        checkSourceAccess(sourceKey);
        return conceptActions.getConceptRecordCount(sourceKey, identifiers);
    }

    // ===========================================================================================
    // Domain report endpoints
    // ===========================================================================================
    /**
     * Get the dashboard report for a data source.
     *
     * @param sourceKey
     *   The key for the data source to query
     * @return The dashboard report for the given data source
     */
    @GetMapping("{sourceKey}/dashboard")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    @AchillesCache(DomainNames.DASHBOARD)
    public CDMDashboard getDashboard(@PathVariable("sourceKey") final String sourceKey) {
        checkSourceAccess(sourceKey);
        return domainReportActions.getRawDashboard(sourceKey);
    }

    /**
     * Get the data density report for a data source.
     *
     * @param sourceKey
     *         The source key
     * @return The data density report
     */
    @GetMapping("{sourceKey}/datadensity")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    @AchillesCache(DomainNames.DATA_DENSITY)
    public CDMDataDensity getDataDensity(@PathVariable("sourceKey") final String sourceKey) {
        checkSourceAccess(sourceKey);
        return domainReportActions.getRawDataDensity(sourceKey);
    }

    /**
     * Get drilldown report for a domain concept in a data source.
     *
     * @param domain
     *         The concept domain (e.g. CONDITION_OCCURRENCE, DRUG_EXPOSURE, etc.)
     * @param conceptId
     *         The concept ID
     * @param sourceKey
     *         The key for the data source to query
     * @return JSON drilldown report
     */
    @GetMapping("{sourceKey}/{domain}/{conceptId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    @AchillesCache(DomainNames.DRILLDOWN)
    public JsonNode getDrilldown(
            @PathVariable("domain") final String domain,
            @PathVariable("conceptId") final int conceptId,
            @PathVariable("sourceKey") final String sourceKey) {
        checkSourceAccess(sourceKey);
        return domainReportActions.getRawDrilldown(domain, conceptId, sourceKey);
    }

    /**
     * Get the death report for a data source
     *
     * @param sourceKey
     *   The key for the data source to query
     * @return The death report for the given data source
     */
    @GetMapping("{sourceKey}/death")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    @AchillesCache(DomainNames.DEATH)
    public CDMDeath getDeath(@PathVariable("sourceKey") final String sourceKey) {
        checkSourceAccess(sourceKey);
        return domainReportActions.getRawDeath(sourceKey);
    }

    /**
     * Get the observation period report for a data source
     * 
     * @param sourceKey
     *   The key for the data source to query
     * @return The observation period report for the given data source
     */
    @GetMapping("{sourceKey}/observationPeriod")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    @AchillesCache(DomainNames.OBSERVATION_PERIOD)
    public CDMObservationPeriod getObservationPeriod(@PathVariable("sourceKey") final String sourceKey) {
        checkSourceAccess(sourceKey);
        return domainReportActions.getRawObservationPeriod(sourceKey);
    }

    /**
     * Get the person report for a data source
     * 
     * @param sourceKey
     *    The key for the data source to query
     * @return The person report for the given data source
     */
    @GetMapping("{sourceKey}/person")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    @AchillesCache(DomainNames.PERSON)
    public CDMPersonSummary getPerson(@PathVariable("sourceKey") final String sourceKey) {
        checkSourceAccess(sourceKey);
        return domainReportActions.getRawPerson(sourceKey);
    }

    /**
     * Get a domain treemap for a data source
     * 
     * @param domain
     *    The domain for the treemap (e.g. CONDITION_OCCURRENCE, DRUG_EXPOSURE, etc.)
     * @param sourceKey
     *    The key for the data source to query
     * @return JSON array of treemap results for the given domain and source
     */
    @GetMapping("{sourceKey}/{domain}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    @AchillesCache(DomainNames.TREEMAP)
    public ArrayNode getTreemap(
            @PathVariable("domain") final String domain,
            @PathVariable("sourceKey") final String sourceKey) {
        checkSourceAccess(sourceKey);
        return domainReportActions.getRawTreeMap(domain, sourceKey);
    }

    private void checkSourceAccess(String sourceKey) {
        if (!authorizationManager.currentUserCanRead(sourceKey)) {
            throw new AccessDeniedException("User does not have permission to access this source");
        }
    }
}
