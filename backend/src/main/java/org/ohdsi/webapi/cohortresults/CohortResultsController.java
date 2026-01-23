package org.ohdsi.webapi.cohortresults;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.cohortanalysis.CohortAnalysis;
import org.ohdsi.webapi.cohortanalysis.CohortSummary;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionService;
import org.ohdsi.webapi.cohortresults.dto.GenerationInfoDTO;
import org.ohdsi.webapi.person.CohortPerson;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.ws.rs.core.Response;

/**
 * REST API for retrieving cohort analyses (a.k.a Heracles Results).
 *
 */

@RestController
@RequestMapping("/cohortresults")
public class CohortResultsController {
    private final CohortResultsService cohortResultsService;
    private final CohortDefinitionService cohortDefinitionService;
    private final AuthorizationManager authorizationManager;

    public CohortResultsController(CohortResultsService cohortResultsService, CohortDefinitionService cohortDefinitionService, AuthorizationManager authorizationManager) {
        this.cohortResultsService = cohortResultsService;
        this.cohortDefinitionService = cohortDefinitionService;
        this.authorizationManager = authorizationManager;
    }

    /**
     * Queries for cohort analysis results for the given cohort definition id
     *
     * @param id
     *         cohort_defintion id
     * @param analysisGroup
     *         Name of the analysisGrouping under the cohortresults/sql/ directory
     * @param analysisName
     *         Name of the analysis, currently the same name as the sql file under analysisGroup
     * @param sourceKey
     *         the source to retrieve results
     * @return List of key, value pairs
     */
    @GetMapping("/{sourceKey}/{id}/raw/{analysis_group}/{analysis_name}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<Map<String, String>> getCohortResultsRaw(
        @PathVariable("id") final int id,
        @PathVariable("analysis_group") final String analysisGroup,
        @PathVariable("analysis_name") final String analysisName,
        @RequestParam("min_covariate_person_count") final Integer minCovariatePersonCountParam,
        @RequestParam("min_interval_person_count") final Integer minIntervalPersonCountParam,
        @PathVariable("sourceKey") String sourceKey
    ) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortResultsRaw(id, analysisGroup, analysisName, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey);
    }

    /**
     * Export the cohort analysis results to a ZIP file
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source Key
     * @return A response containing the .ZIP file of results
     */
    @GetMapping("/{sourceKey}/{id}/export.zip")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Response exportCohortResults(@PathVariable("id") int id, @PathVariable("sourceKey") String sourceKey) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.exportCohortResults(id, sourceKey);
    }

    /**
     * Provides a list of cohort analysis visualizations that are completed
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @return A list of visualization keys that are complete
     */
    @GetMapping("/{sourceKey}/{id}/completed")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<String> getCompletedVisualizations(@PathVariable("id") final int id,
            @PathVariable("sourceKey") final String sourceKey) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCompletedVisualizations(id, sourceKey);
    }

    /**
     * Get the completed analyses IDs for the selected cohort and source key
     *
     * @param sourceKey
     *         The source key
     * @param id
     *         The cohort ID
     * @return A list of completed analysis IDs
     */
    @GetMapping("/{sourceKey}/{id}/analyses")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<Integer> getCompletedAnalyses(
            @PathVariable("sourceKey") String sourceKey,
            @PathVariable("id") int id) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCompletedAnalyses(sourceKey, id);
    }


    /**
     * Get the analysis generation progress
     *
     * @param sourceKey
     *         The source key
     * @param id
     *         The cohort ID
     * @return The generation progress information
     */
    @GetMapping("/{sourceKey}/{id}/info")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public GenerationInfoDTO getAnalysisProgress(
            @PathVariable("sourceKey") String sourceKey,
            @PathVariable("id") Integer id) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getAnalysisProgress(sourceKey, id);
    }

    /**
     * Retrieves the tornado plot
     *
     * @param sourceKey
     *         The source key
     * @param cohortDefinitionId
     *         The cohort definition id
     * @return The tornado plot data
     */
    @GetMapping("/{sourceKey}/{id}/tornado")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public TornadoReport getTornadoReport(
            @PathVariable("sourceKey") final String sourceKey,
            @PathVariable("id") final int cohortDefinitionId) {
        checkPermissions(cohortDefinitionId, sourceKey);
        return cohortResultsService.getTornadoReport(sourceKey, cohortDefinitionId);
    }

    /**
     * Queries for cohort analysis dashboard for the given cohort definition id
     *
     * @param id
     *         The cohort definition id
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param demographicsOnly
     *         only render gender and age
     * @return CohortDashboard
     */
    @GetMapping("/{sourceKey}/{id}/dashboard")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortDashboard getDashboard(@PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @RequestParam(name = "demographics_only", required = false) final boolean demographicsOnly,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getDashboard(id, minCovariatePersonCountParam, minIntervalPersonCountParam, demographicsOnly, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis condition treemap results for the given cohort definition id
     *
     * @param sourceKey
     *         The source key
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param refresh
     *         Boolean - refresh visualization data
     * @return List of HierarchicalConceptRecord
     */
    @GetMapping("/{sourceKey}/{id}/condition")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<HierarchicalConceptRecord> getConditionTreemap(@PathVariable("sourceKey") String sourceKey,
            @PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getConditionTreemap(sourceKey, id, minCovariatePersonCountParam, minIntervalPersonCountParam, refresh);
    }

    /**
     * Get the distinct person count for a cohort
     *
     * @param sourceKey
     *         The source key
     * @param id
     *         The cohort ID
     * @param refresh
     *         Boolean - refresh visualization data
     * @return Distinct person count as integer
     */
    @GetMapping("/{sourceKey}/{id}/distinctPersonCount")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Integer getRawDistinctPersonCount(@PathVariable("sourceKey") String sourceKey,
            @PathVariable("id") int id,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getRawDistinctPersonCount(sourceKey, id, refresh);
    }

    /**
     * Queries for cohort analysis condition drilldown results for the given cohort definition id and condition id
     *
     * @param sourceKey
     *         The source key
     * @param id
     *         The cohort ID
     * @param conditionId
     *         The condition concept ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param refresh
     *         Boolean - refresh visualization data
     * @return The CohortConditionDrilldown detail object
     */
    @GetMapping("/{sourceKey}/{id}/condition/{conditionId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortConditionDrilldown getConditionResults(@PathVariable("sourceKey") String sourceKey,
            @PathVariable("id") final int id,
            @PathVariable("conditionId") final int conditionId,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getConditionResults(sourceKey, id, conditionId, minCovariatePersonCountParam, minIntervalPersonCountParam, refresh);
    }

    /**
     * Queries for cohort analysis condition era treemap results for the given cohort definition id
     *
     * @param sourceKey
     *         The source key
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param refresh
     *         Boolean - refresh visualization data
     * @return List of HierarchicalConceptRecord
     */
    @GetMapping("/{sourceKey}/{id}/conditionera")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<HierarchicalConceptRecord> getConditionEraTreemap(@PathVariable("sourceKey") final String sourceKey,
            @PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getConditionEraTreemap(sourceKey, id, minCovariatePersonCountParam, minIntervalPersonCountParam, refresh);
    }

    /**
     * Queries for cohort analysis condition era drilldown results for the given cohort definition id and condition id
     *
     * @param id
     *         The cohort ID
     * @param conditionId
     *         The condition ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return The CohortConditionEraDrilldown object
     */
    @GetMapping("/{sourceKey}/{id}/conditionera/{conditionId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortConditionEraDrilldown getConditionEraDrilldown(@PathVariable("id") final int id,
            @PathVariable("conditionId") final int conditionId,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getConditionEraDrilldown(id, conditionId, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for drug analysis treemap results for the given cohort definition id
     *
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return List of HierarchicalConceptRecord
     */
    @GetMapping("/{sourceKey}/{id}/drug")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<HierarchicalConceptRecord> getDrugTreemap(@PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getDrugTreemap(id, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis drug drilldown results for the given cohort definition id and drug id
     *
     * @param id
     *         The cohort ID
     * @param drugId
     *         The drug concept ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return
     */
    @GetMapping("/{sourceKey}/{id}/drug/{drugId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortDrugDrilldown getDrugResults(@PathVariable("id") final int id, @PathVariable("drugId") final int drugId,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {

        checkPermissions(id, sourceKey);
        return cohortResultsService.getDrugResults(id, drugId, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis drug era treemap results for the given cohort definition id
     *
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return List of HierarchicalConceptRecord
     */
    @GetMapping("/{sourceKey}/{id}/drugera")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<HierarchicalConceptRecord> getDrugEraTreemap(@PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getDrugEraTreemap(id, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis drug era drilldown results for the given cohort definition id and drug id
     *
     * @param id
     *         The cohort ID
     * @param drugId
     *         The drug concept ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return CohortDrugEraDrilldown
     */
    @GetMapping("/{sourceKey}/{id}/drugera/{drugId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortDrugEraDrilldown getDrugEraResults(@PathVariable("id") final int id,
            @PathVariable("drugId") final int drugId,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getDrugEraResults(id, drugId, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis person results for the given cohort definition id
     *
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return
     */
    @GetMapping("/{sourceKey}/{id}/person")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortPersonSummary getPersonResults(@PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getPersonResults(id, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis cohort specific results for the given cohort definition id
     *
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return CohortSpecificSummary
     */
    @GetMapping("/{sourceKey}/{id}/cohortspecific")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortSpecificSummary getCohortSpecificResults(@PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortSpecificResults(id, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis cohort specific treemap results for the given cohort definition id
     *
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return CohortSpecificTreemap
     */
    @GetMapping("/{sourceKey}/{id}/cohortspecifictreemap")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortSpecificTreemap getCohortSpecificTreemapResults(@PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortSpecificTreemapResults(id, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis procedure drilldown results for the given cohort definition id and concept id
     *
     * @param id
     *         The cohort ID
     * @param conceptId
     *         The procedure concept ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return List of ScatterplotRecord
     */
    @GetMapping("/{sourceKey}/{id}/cohortspecificprocedure/{conceptId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<ScatterplotRecord> getCohortProcedureDrilldown(@PathVariable("id") final int id,
            @PathVariable("conceptId") final int conceptId,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortProcedureDrilldown(id, conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis drug drilldown results for the given cohort definition id and concept id
     *
     * @param id
     *         The cohort ID
     * @param conceptId
     *         The drug concept ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return List of ScatterplotRecord
     */
    @GetMapping("/{sourceKey}/{id}/cohortspecificdrug/{conceptId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<ScatterplotRecord> getCohortDrugDrilldown(@PathVariable("id") final int id,
            @PathVariable("conceptId") final int conceptId,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortDrugDrilldown(id, conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis condition drilldown results for the given cohort definition id and concept id
     *
     * @param id
     *         The cohort ID
     * @param conceptId
     *         The condition concept ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return List of ScatterplotRecord
     */
    @GetMapping("/{sourceKey}/{id}/cohortspecificcondition/{conceptId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<ScatterplotRecord> getCohortConditionDrilldown(@PathVariable("id") final int id,
            @PathVariable("conceptId") final int conceptId,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortConditionDrilldown(id, conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis for observation treemap
     *
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return List of HierarchicalConceptRecord
     */
    @GetMapping("/{sourceKey}/{id}/observation")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<HierarchicalConceptRecord> getCohortObservationResults(@PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count") final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count") final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortObservationResults(id, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis observation drilldown results for the given cohort definition id and observation
     * concept id
     *
     * @param id
     *         The cohort ID
     * @param conceptId
     *         The observation concept ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return CohortObservationDrilldown
     */
    @GetMapping("/{sourceKey}/{id}/observation/{conceptId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortObservationDrilldown getCohortObservationResultsDrilldown(@PathVariable("id") final int id,
            @PathVariable("conceptId") final int conceptId,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortObservationResultsDrilldown(id, conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis for measurement treemap
     *
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return List of HierarchicalConceptRecord
     */
    @GetMapping("/{sourceKey}/{id}/measurement")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<HierarchicalConceptRecord> getCohortMeasurementResults(@PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
           @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortMeasurementResults(id, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis measurement drilldown results for the given cohort definition id and measurement
     * concept id
     *
     * @param id
     *         The cohort ID
     * @param conceptId
     *         The measurement concept ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return CohortMeasurementDrilldown
     */
    @GetMapping("/{sourceKey}/{id}/measurement/{conceptId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortMeasurementDrilldown getCohortMeasurementResultsDrilldown(@PathVariable("id") final int id,
            @PathVariable("conceptId") final int conceptId,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortMeasurementResultsDrilldown(id, conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis observation period for the given cohort definition id
     *
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return CohortObservationPeriod
     */
    @GetMapping("/{sourceKey}/{id}/observationperiod")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortObservationPeriod getCohortObservationPeriod(@PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortObservationPeriod(id, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis data density for the given cohort definition id
     *
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return CohortDataDensity
     */
    @GetMapping("/{sourceKey}/{id}/datadensity")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortDataDensity getCohortDataDensity(@PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortDataDensity(id, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis procedure treemap results for the given cohort definition id
     *
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return a list of HierarchicalConceptRecords
     */
    @GetMapping("/{sourceKey}/{id}/procedure")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<HierarchicalConceptRecord> getProcedureTreemap(@PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getProcedureTreemap(id, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis procedures for the given cohort definition id and concept id
     *
     * @param id
     *         The cohort ID
     * @param conceptId
     *         The procedure concept ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return CohortProceduresDrillDown
     */
    @GetMapping("/{sourceKey}/{id}/procedure/{conceptId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortProceduresDrillDown getCohortProceduresDrilldown(@PathVariable("id") final int id,
            @PathVariable("conceptId") final int conceptId,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortProceduresDrilldown(id, conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis visit treemap results for the given cohort definition id
     *
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return a list of HierarchicalConceptRecords
     */
    @GetMapping("/{sourceKey}/{id}/visit")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<HierarchicalConceptRecord> getVisitTreemap(@PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count") final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count") final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getVisitTreemap(id, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Queries for cohort analysis visits for the given cohort definition id and concept id
     *
     * @param id
     *         The cohort ID
     * @param conceptId
     *         The visit concept iD
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return
     */
    @GetMapping("/{sourceKey}/{id}/visit/{conceptId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortVisitsDrilldown getCohortVisitsDrilldown(@PathVariable("id") final int id,
            @PathVariable("conceptId") final int conceptId,
            @RequestParam(name = "min_covariate_person_count") final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count") final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortVisitsDrilldown(id, conceptId, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Returns the summary for the cohort
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @return CohortSummary
     */
    @GetMapping("/{sourceKey}/{id}/summarydata")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortSummary getCohortSummaryData(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") String sourceKey) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortSummaryData(id, sourceKey);
    }

    /**
     * Queries for cohort analysis death data for the given cohort definition id
     *
     * @param id
     *         The cohort ID
     * @param minCovariatePersonCountParam
     *         The minimum number of covariates per person
     * @param minIntervalPersonCountParam
     *         The minimum interval person count
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return CohortDeathData
     */
    @GetMapping("/{sourceKey}/{id}/death")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortDeathData getCohortDeathData(@PathVariable("id") final int id,
            @RequestParam(name = "min_covariate_person_count", required = false) final Integer minCovariatePersonCountParam,
            @RequestParam(name = "min_interval_person_count", required = false) final Integer minIntervalPersonCountParam,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortDeathData(id, minCovariatePersonCountParam, minIntervalPersonCountParam, sourceKey, refresh);
    }

    /**
     * Returns the summary for the cohort
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @return CohortSummary
     */
    @GetMapping("/{sourceKey}/{id}/summaryanalyses")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public CohortSummary getCohortSummaryAnalyses(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") String sourceKey) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortSummaryAnalyses(id, sourceKey);
    }

    /**
     * Returns the person identifiers of all members of a generated cohort definition identifier
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @param min
     *         The minimum number of persons to return
     * @param max
     *         The maximum number of persons to return
     * @return Collection of CohortPerson
     */
    @GetMapping("/{sourceKey}/{id}/members/{min}-{max}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<CohortPerson> getCohortMembers(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") String sourceKey,
            @PathVariable("min") final int min,
            @PathVariable("max") final int max) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortMembers(id, sourceKey, min, max);
    }

    /**
     * Returns breakdown with counts about people in cohort
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @return Collection of CohortBreakdown
     */
    @GetMapping("/{sourceKey}/{id}/breakdown")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<CohortBreakdown> getCohortBreakdown(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") String sourceKey) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortBreakdown(id, sourceKey);
    }

    /**
     * Returns the person identifiers of all members of a cohort breakdown from above
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @param gender
     *         The string for gender (male/female)
     * @param age
     *         The numeric age
     * @param conditions
     *         The condition concept IDs
     * @param drugs
     *         The drug concept ids
     * @param rows
     *         The row limit
     * @return List of all members of a generated cohort definition identifier
     */
    @GetMapping("/{sourceKey}/{id}/breakdown/{gender}/{age}/{conditions}/{drugs}/{rows}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<CohortPerson> getCohortMembers(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") String sourceKey,
            @PathVariable("gender") String gender,
            @PathVariable("age") String age,
            @PathVariable("conditions") String conditions,
            @PathVariable("drugs") String drugs,
            @PathVariable("rows") final int rows) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortMembers(id, sourceKey, gender, age, conditions, drugs, rows);
    }

    /**
     * Returns the count of all members of a generated cohort definition identifier
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @return The cohort count
     */
    @GetMapping("/{sourceKey}/{id}/members/count")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Long getCohortMemberCount(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") String sourceKey) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortMemberCount(id, sourceKey);
    }

    /**
     * Returns all cohort analyses in the results/OHDSI schema for the given cohort_definition_id
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @param retrieveFullDetail
     *         Boolean - when TRUE, the full analysis details are returned
     * @return List of all cohort analyses and their statuses for the given cohort_defintion_id
     */
    @GetMapping("/{sourceKey}/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<CohortAnalysis> getCohortAnalysesForCohortDefinition(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") String sourceKey,
            @RequestParam(name="fullDetail", defaultValue="true") boolean retrieveFullDetail) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getCohortAnalysesForCohortDefinition(id, sourceKey, retrieveFullDetail);
    }

    /**
     * Get the exposure cohort incidence rates. This function is not using a proper incidence rate so this should be
     * viewed as informational only and not as a report
     *
     * @param sourceKey
     *         The source key
     * @param search
     *         The exposure cohort search
     * @return List of ExposureCohortResult
     * @deprecated
     */
    @PostMapping("/{sourceKey}/exposurecohortrates")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    @Deprecated
    public List<ExposureCohortResult> getExposureOutcomeCohortRates(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody ExposureCohortSearch search) {
        if (!authorizationManager.currentUserCanRead(sourceKey)) {
            throw new AccessDeniedException("You do not have permission to access the data source");
        }
        List<Integer> cohortDefinitionIds = new ArrayList<>();
        cohortDefinitionIds.addAll(Arrays.stream(search.exposureCohortList).map(Integer::parseInt).collect(Collectors.toList()));
        cohortDefinitionIds.addAll(Arrays.stream(search.outcomeCohortList).map(Integer::parseInt).collect(Collectors.toList()));
        if (!cohortDefinitionIds.stream().allMatch(id -> authorizationManager.currentUserCanRead(cohortDefinitionService.getCohortDefinition(id)))) {
            throw new AccessDeniedException("You do not have permission to access one or more of the cohort definitions");
        }

        return cohortResultsService.getExposureOutcomeCohortRates(sourceKey, search);
    }

    /**
     * Provides a time to event calculation but it is unclear how this works.
     *
     * @param sourceKey
     *         The source key
     * @param search
     *         The exposure cohort search
     * @return List of TimeToEventResult
     * @deprecated
     */
    @PostMapping("/{sourceKey}/timetoevent")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    @Deprecated
    public List<TimeToEventResult> getTimeToEventDrilldown(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody ExposureCohortSearch search) {
        if (!authorizationManager.currentUserCanRead(sourceKey)) {
            throw new AccessDeniedException("You do not have permission to access the data source");
        }
        List<Integer> cohortDefinitionIds = new ArrayList<>();
        cohortDefinitionIds
                .addAll(Arrays.stream(search.exposureCohortList).map(Integer::parseInt).collect(Collectors.toList()));
        cohortDefinitionIds
                .addAll(Arrays.stream(search.outcomeCohortList).map(Integer::parseInt).collect(Collectors.toList()));
        if (!cohortDefinitionIds.stream().allMatch(
                id -> authorizationManager.currentUserCanRead(cohortDefinitionService.getCohortDefinition(id)))) {
            throw new AccessDeniedException(
                    "You do not have permission to access one or more of the cohort definitions");
        }
        return cohortResultsService.getTimeToEventDrilldown(sourceKey, search);
    }


    /**
     * Provides a predictor calculation but it is unclear how this works.
     *
     * @param sourceKey
     *         The source key
     * @param search
     *         The exposure cohort search
     * @return List of PredictorResult
     * @deprecated
     */
    @PostMapping("/{sourceKey}/predictors")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    @Deprecated
    public List<PredictorResult> getExposureOutcomeCohortPredictors(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody ExposureCohortSearch search) {
        if (!authorizationManager.currentUserCanRead(sourceKey)) {
            throw new AccessDeniedException("You do not have permission to access the data source");
        }
        List<Integer> cohortDefinitionIds = new ArrayList<>();
        cohortDefinitionIds
                .addAll(Arrays.stream(search.exposureCohortList).map(Integer::parseInt).collect(Collectors.toList()));
        cohortDefinitionIds
                .addAll(Arrays.stream(search.outcomeCohortList).map(Integer::parseInt).collect(Collectors.toList()));
        if (!cohortDefinitionIds.stream().allMatch(
                id -> authorizationManager.currentUserCanRead(cohortDefinitionService.getCohortDefinition(id)))) {
            throw new AccessDeniedException(
                    "You do not have permission to access one or more of the cohort definitions");
        }
        return cohortResultsService.getExposureOutcomeCohortPredictors(sourceKey, search);
    }

    /**
     * Returns heracles heel results (data quality issues) for the given cohort definition id
     *
     * @param id
     *         The cohort iD
     * @param sourceKey
     *         The source key
     * @param refresh
     *         Boolean - refresh visualization data
     * @return List of CohortAttribute
     */
    @GetMapping("/{sourceKey}/{id}/heraclesheel")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<CohortAttribute> getHeraclesHeel(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name="refresh", defaultValue="false") boolean refresh) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getHeraclesHeel(id, sourceKey, refresh);
    }

    /**
     * Provides a data completeness report for a cohort
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @return List of DataCompletenessAttr
     */
    @GetMapping("/{sourceKey}/{id}/datacompleteness")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<DataCompletenessAttr> getDataCompleteness(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") String sourceKey) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getDataCompleteness(id, sourceKey);
    }

    /**
     * Provide an entropy report for a cohort
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @return List of EntropyAttr
     */
    @GetMapping("/{sourceKey}/{id}/entropy")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<EntropyAttr> getEntropy(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") String sourceKey) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getEntropy(id, sourceKey);
    }

    /**
     * Provide a full entropy report for a cohort
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @return List of EntropyAttr
     */
    @GetMapping("/{sourceKey}/{id}/allentropy")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<EntropyAttr> getAllEntropy(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") String sourceKey) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getAllEntropy(id, sourceKey);
    }

    /**
     * Get the healthcare utilization exposure report for a specific window
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @param windowString
     *         The time window
     * @param periodTypeString
     *         The period type
     * @return HealthcareExposureReport
     */
    @GetMapping("/{sourceKey}/{id}/healthcareutilization/exposure/{window}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public HealthcareExposureReport getHealthcareUtilizationExposureReport(
        @PathVariable("id") final int id,
        @PathVariable("sourceKey") String sourceKey,
        @PathVariable("window") final String windowString,
        @RequestParam(name="periodType", defaultValue="ww") final String periodTypeString
    ) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getHealthcareUtilizationExposureReport(id, sourceKey, windowString, periodTypeString);
    }

    /**
     * Get the healthcare utilization periods
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @param windowString
     *         The time window
     * @return A list of the periods
     */
    @GetMapping("/{sourceKey}/{id}/healthcareutilization/periods/{window}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<String> getHealthcareUtilizationPeriods(
        @PathVariable("id") final int id,
        @PathVariable("sourceKey") final String sourceKey,
        @PathVariable("window") final String windowString) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getHealthcareUtilizationPeriods(id, sourceKey, windowString);
    }

    /**
     * Get the healthcare utilization report by window, visit status, period type, visit concept, visit type concept and
     * cost type concept.
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @param windowString
     *         The time window
     * @param visitStatString
     *         The visit status
     * @param periodTypeString
     *         The period type
     * @param visitConcept
     *         The visit concept ID
     * @param visitTypeConcept
     *         The visit type concept ID
     * @param costTypeConcept
     *         The cost type concept ID
     * @return HealthcareVisitUtilizationReport
     */
    @GetMapping("/{sourceKey}/{id}/healthcareutilization/visit/{window}/{visitStat}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public HealthcareVisitUtilizationReport getHealthcareUtilizationVisitReport(
        @PathVariable("id") final int id,
        @PathVariable("sourceKey") String sourceKey,
        @PathVariable("window") final String windowString,
        @PathVariable("visitStat") final String visitStatString,
        @RequestParam(name="periodType", defaultValue="ww") final String periodTypeString,
        @RequestParam(name = "visitConcept", required = false) final Long visitConcept,
        @RequestParam(name = "visitTypeConcept", required = false) final Long visitTypeConcept,
        @RequestParam(name="costTypeConcept", defaultValue="31968") final Long costTypeConcept
    ) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getHealthcareUtilizationVisitReport(
            id, sourceKey, windowString, visitStatString, periodTypeString, visitConcept, visitTypeConcept, costTypeConcept);
    }

    /**
     * Get the healthcare utilization summary report by drug and cost type concept
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @param windowString
     *         The time window
     * @param drugTypeConceptId
     *         The drug type concept ID
     * @param costTypeConceptId
     *         The cost type concept ID
     * @return HealthcareDrugUtilizationSummary
     */
    @GetMapping("/{sourceKey}/{id}/healthcareutilization/drug/{window}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public HealthcareDrugUtilizationSummary getHealthcareUtilizationDrugSummaryReport(
        @PathVariable("id") final int id,
        @PathVariable("sourceKey") String sourceKey,
        @PathVariable("window") final String windowString,
        @RequestParam(name = "drugType", required = false) final Long drugTypeConceptId,
        @RequestParam(name = "costType", defaultValue = "31968") final Long costTypeConceptId
    ) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getHealthcareUtilizationDrugSummaryReport(
            id, sourceKey, windowString, drugTypeConceptId, costTypeConceptId);
    }

    /**
     * Get the healthcare utilization detail report by drug and cost type concept
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @param windowString
     *         The time window
     * @param drugConceptId
     *         The drug concept ID
     * @param periodTypeString
     *         The period type
     * @param drugTypeConceptId
     *         The drug type concept ID
     * @param costTypeConceptId
     *         The cost type concept ID
     * @return HealthcareDrugUtilizationDetail
     */
    @GetMapping("/{sourceKey}/{id}/healthcareutilization/drug/{window}/{drugConceptId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public HealthcareDrugUtilizationDetail getHealthcareUtilizationDrugDetailReport(
        @PathVariable("id") final int id,
        @PathVariable("sourceKey") String sourceKey,
        @PathVariable("window") final String windowString,
        @PathVariable("drugConceptId") final Long drugConceptId,
        @RequestParam(name="periodType", defaultValue="ww") final String periodTypeString,
        @RequestParam("drugType") final Long drugTypeConceptId,
        @RequestParam(name="costType", defaultValue="31968") final Long costTypeConceptId
    ) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getHealthcareUtilizationDrugDetailReport(
            id, sourceKey, windowString, drugConceptId, periodTypeString, drugTypeConceptId, costTypeConceptId);
    }

    /**
     * Get the drug type concepts for the selected drug concept ID
     *
     * @param id
     *         The cohort ID
     * @param sourceKey
     *         The source key
     * @param drugConceptId
     *         The drug concept ID
     * @return A list of concepts of drug types
     */
    @GetMapping("/{sourceKey}/{id}/healthcareutilization/drugtypes")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public List<Concept> getDrugTypes(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") String sourceKey,
            @RequestParam(name = "drugConceptId", required = false) final Long drugConceptId) {
        checkPermissions(id, sourceKey);
        return cohortResultsService.getDrugTypes(id, sourceKey, drugConceptId);
    }



    private void checkPermissions(int cohortDefinitionId, String sourceKey) {
        var cohortDefinition = cohortDefinitionService.getCohortDefinition(cohortDefinitionId);
        if (!authorizationManager.currentUserCanRead(cohortDefinition) ||
            !authorizationManager.currentUserCanRead(sourceKey)) {
            throw new AccessDeniedException("You do not have permission to access the cohort definition and/or data source");
        }
    }
}
