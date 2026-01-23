package org.ohdsi.webapi.cohortanalysis;

import java.util.List;
import java.util.stream.Collectors;

import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionService;
import org.ohdsi.webapi.job.JobExecutionResource;
import org.ohdsi.webapi.model.results.Analysis;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for running cohort (a.k.a Heracles) analyses.
 *
 */
@RestController
@RequestMapping("/cohortanalysis")
public class CohortAnalysisController {
    private final CohortAnalysisService analysisService;
    private final CohortDefinitionService cohortDefinitionService;
    private final AuthorizationManager authorizationManager;

    public CohortAnalysisController(CohortAnalysisService analysisService, CohortDefinitionService cohortDefinitionService, AuthorizationManager authorizationManager) {
        this.analysisService = analysisService;
        this.cohortDefinitionService = cohortDefinitionService;
        this.authorizationManager = authorizationManager;
    }

    /**
     * Returns all cohort analyses in the WebAPI database
     *
     * @return List of all cohort analyses
     */
    @GetMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    public List<Analysis> getCohortAnalyses() {
        var cohortDefinitions = cohortDefinitionService.getAllCohortDefinitions();
        if (!cohortDefinitions.stream().allMatch(cd -> authorizationManager.currentUserCanRead(cd))) {
            throw new AccessDeniedException("You do not have permission to access one or more cohort definitions.");
        }
        return analysisService.getCohortAnalyses();
    }

    /**
     * Returns all cohort analyses in the WebAPI database for the given cohort_definition_id
     *
     * @param id
     *         The cohort definition identifier
     * @return List of all cohort analyses and their statuses for the given cohort_definition_id
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    public List<CohortAnalysis> getCohortAnalysesForCohortDefinition(@PathVariable("id") final int id) {
        checkCohortDefinitionAccess(id);
        return analysisService.getCohortAnalysesForCohortDefinition(id);
    }

    /**
     * Returns the summary for the cohort
     *
     * @param id
     *         - the cohort_definition id
     * @return Summary which includes the base cohort_definition, the cohort analyses list and their statuses for this
     *         cohort, and a base set of common cohort results that may or may not yet have been ran
     */
    @GetMapping("/{id}/summary")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    public CohortSummary getCohortSummary(@PathVariable("id") final int id) {
        checkCohortDefinitionAccess(id);
        return analysisService.getCohortSummary(id);
    }

    /**
     * Generates a preview of the cohort analysis SQL used to run the Cohort Analysis Job
     *
     * @param task
     *         - the CohortAnalysisTask, be sure to have a least one analysis_id and one cohort_definition id
     * @return - SQL for the given CohortAnalysisTask translated and rendered to the current dialect
     */
    @PostMapping("/preview")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    public String getRunCohortAnalysisSql(@RequestBody CohortAnalysisTask task) {
        var cohortDefinitionIds = task.getCohortDefinitionIds()
            .stream()
            .map(Integer::parseInt)
            .collect(Collectors.toList());
        var cohortDefinitions = cohortDefinitionService.getCohortDefinitions(cohortDefinitionIds);
        if (!cohortDefinitions.stream().allMatch(cd -> authorizationManager.currentUserCanRead(cd))) {
            throw new AccessDeniedException("You do not have permission to access one or more cohort definitions.");
        }
        return analysisService.getRunCohortAnalysisSql(task);
    }

    /**
     * Queues up a cohort analysis task, that generates and translates SQL for the given cohort definitions, analysis
     * ids and concept ids
     *
     * @param task
     *         The cohort analysis task to be ran
     * @return information about the Cohort Analysis Job
     * @throws Exception
     */
    @PostMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public JobExecutionResource queueCohortAnalysisJob(@RequestBody CohortAnalysisTask task) throws Exception {
        // TODO: The UI is passing a job name and a small cell count value that it is hard coded.
        // The small cell count value should be left off unless we make it configurable in the UI.
        // The job name should be created here in the backend.
        var cohortDefinitionIds = task.getCohortDefinitionIds()
                .stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        var cohortDefinitions = cohortDefinitionService.getCohortDefinitions(cohortDefinitionIds);
        if (!cohortDefinitions.stream().allMatch(cd -> authorizationManager.currentUserCanUpdate(cd))) {
            throw new AccessDeniedException("You do not have permission to update one or more cohort definitions.");
        }
        var source = task.getSourceKey();
        if (!authorizationManager.currentUserCanRead(source)) {
            throw new AccessDeniedException("You do not have permission to read the specified source.");
        }
        return analysisService.queueCohortAnalysisJob(task);
    }

    private void checkCohortDefinitionAccess(int id) {
        var cohortDefinition = cohortDefinitionService.getCohortDefinition(id);
        if (!authorizationManager.currentUserCanRead(cohortDefinition)) {
            throw new AccessDeniedException("You do not have permission to access the specified cohort definition.");
        }
    }

}
