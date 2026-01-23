/*
 * Copyright 2015 Observational Health Data Sciences and Informatics [OHDSI.org].
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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.ohdsi.circe.check.Checker;
import org.ohdsi.circe.cohortdefinition.CohortExpression;
import org.ohdsi.circe.cohortdefinition.CohortExpressionQueryBuilder;
import org.ohdsi.circe.cohortdefinition.ConceptSet;
import org.ohdsi.circe.cohortdefinition.printfriendly.MarkdownRender;
import org.ohdsi.webapi.authorization.model.SecuredEntityType;
import org.ohdsi.webapi.check.CheckResult;
import org.ohdsi.webapi.check.checker.cohort.CohortChecker;
import org.ohdsi.webapi.check.warning.WarningUtils;
import org.ohdsi.webapi.cohortdefinition.converter.CohortGenerationInfoToCohortGenerationInfoDTOConverter;
import org.ohdsi.webapi.cohortdefinition.converter.CohortRawDTOToCohortDTOConverter;
import org.ohdsi.webapi.cohortdefinition.converter.CohortVersionToCohortVersionFullDTOConverter;
import org.ohdsi.webapi.cohortdefinition.dto.*;
import org.ohdsi.webapi.cohortgeneration.CohortGenerationService;
import org.ohdsi.webapi.common.generation.GenerateSqlResult;
import org.ohdsi.webapi.job.JobExecutionResource;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.source.SourceInfo;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.tag.dto.TagNameListRequestDTO;
import org.ohdsi.webapi.util.*;
import org.ohdsi.webapi.versioning.VersionBaseToVersionDTOConverter;
import org.ohdsi.webapi.versioning.VersionToVersionDTOConverter;
import org.ohdsi.webapi.versioning.domain.CohortVersion;
import org.ohdsi.webapi.versioning.dto.VersionDTO;
import org.ohdsi.webapi.versioning.dto.VersionUpdateDTO;
import org.ohdsi.webapi.versioning.service.CohortDefinitionVersionService;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for working with cohort definitions.
 *
 */
@RestController
@RequestMapping("/cohortdefinition")
public class CohortDefinitionController {
    private final AuthorizationManager authorizationManager;
    private final CohortDefinitionService cohortDefinitionService;
    private final CohortChecker cohortChecker;
    private final CohortGenerationService cohortGenerationService;
    private final SourceService sourceService;
    private final CohortDefinitionVersionService versionService;
    private final WebApiProperties webApiProperties;
    private final ConversionService conversionService;
    private final CohortVersionToCohortVersionFullDTOConverter cohortVersionToCohortVersionFullDTOConverter;

    private final MarkdownRender markdownPF = new MarkdownRender();
    private final List<Extension> extensions = List.of(TablesExtension.create());

    public CohortDefinitionController(
            AuthorizationManager authorizationManager,
            CohortDefinitionService cohortDefinitionService,
            CohortChecker cohortChecker,
            CohortGenerationService cohortGenerationService,
            SourceService sourceService,
            CohortDefinitionVersionService versionService,
            WebApiProperties webApiProperties,
            ConversionService conversionService,
            CohortVersionToCohortVersionFullDTOConverter cohortVersionToCohortVersionFullDTOConverter) {
        this.authorizationManager = authorizationManager;
        this.cohortDefinitionService = cohortDefinitionService;
        this.cohortChecker = cohortChecker;
        this.cohortGenerationService = cohortGenerationService;
        this.sourceService = sourceService;
        this.versionService = versionService;
        this.webApiProperties = webApiProperties;
        this.conversionService = conversionService;
        this.cohortVersionToCohortVersionFullDTOConverter = cohortVersionToCohortVersionFullDTOConverter;
    }

    public static class GenerateSqlRequest {
        public GenerateSqlRequest() {}

        @JsonProperty("expression")
        public CohortExpression expression;

        @JsonProperty("options")
        public CohortExpressionQueryBuilder.BuildExpressionQueryOptions options;
    }

    /**
     * Returns OHDSI template SQL for a given cohort definition
     *
     * @param request
     *         A GenerateSqlRequest containing the cohort expression and options.
     * @return The OHDSI template SQL needed to generate the input cohort definition as a character string
     */
    @PostMapping("/sql")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    public GenerateSqlResult generateSql(@RequestBody GenerateSqlRequest request) {
        return cohortDefinitionService.generateSql(request);
    }

    /**
     * Returns metadata about all cohort definitions in the WebAPI database
     *
     * @return List of metadata about all cohort definitions in WebAPI
     * see org.ohdsi.webapi.cohortdefinition.CohortMetadataDTO
     */
    @GetMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    @Transactional
    public List<CohortMetadataDTO> getCohortDefinitionList() {
        var definitions = cohortDefinitionService.getAllCohortDefinitions();
        return definitions.stream()
                .filter(!webApiProperties.security().defaultGlobalReadPermissions()
                        ? authorizationManager::currentUserCanRead
                        : entity -> true)
                .map(def -> {
                    var dto = conversionService.convert(def, CohortMetadataImplDTO.class);
                    authorizationManager.fillPermissions(def, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Creates a cohort definition in the WebAPI database.
     * <p>
     * The values for createdBy and createdDate are automatically populated and the function
     * ignores parameter values for these fields.
     *
     * @param dto
     *         The cohort definition to create.
     * @return The newly created cohort definition
     */
    @PostMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.CREATE_COHORT_DEFINITION)")
    @Transactional
    public CohortDTO createCohortDefinition(@RequestBody CohortDTO dto) {
        var newCohortDefinition = cohortDefinitionService.createCohortDefinition(dto);
        var createdDto = conversionService.convert(newCohortDefinition, CohortDTO.class);
        authorizationManager.fillPermissions(newCohortDefinition, createdDto);
        return createdDto;
    }

    /**
     * Returns the 'raw' cohort definition for the given id.
     * <p>
     * 'Raw' means that the cohort expression is returned as a string, and not as a
     * concrete CohortExpression class. This method is maintained for legacy comparability.
     *
     * @param id
     *         The cohort definition id
     * @return The cohort definition JSON expression
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    @Transactional
    public CohortRawDTO getCohortDefinitionRaw(@PathVariable("id") final int id) {
        checkReadPermission(id);
        var d = cohortDefinitionService.getCohortDefinitionWithDetail(id);
        if (d == null) {
            throw new NotFoundException("There is no cohort definition with id = " + id);
        }
        return conversionService.convert(d, CohortRawDTO.class);
    }

    /**
     * Check that a cohort exists.
     * <p>
     * This method checks to see if a cohort definition name exists. The id parameter is used to
     * 'ignore' a cohort definition from checking. This is used when you have an existing cohort
     * definition which should be ignored when checking if the name already exists.
     *
     * @param id
     *         The cohort definition id
     * @param name
     *         The cohort definition name
     * @return 1 if a cohort with the given name and id exist in WebAPI and 0 otherwise
     */
    @GetMapping("/{id}/exists")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    public int getCountCDefWithSameName(
            @PathVariable("id") final int id,
            @RequestParam("name") String name) {
        return cohortDefinitionService.getCountCDefWithSameName(id, name);
    }

    /**
     * Saves the cohort definition for the given id.
     * <p>
     * The modifiedBy and modifiedDate are set automatically, and those values submitted in
     * CohortDTO will be ignored.
     *
     *
     * @param id
     *         The cohort definition id
     * @return The updated CohortDefinition
     */
    @PutMapping("/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_COHORT_DEFINITION)")
    @Transactional
    public CohortDTO saveCohortDefinition(@PathVariable("id") final int id, @RequestBody CohortDTO def) {
        if (!authorizationManager.currentUserCanUpdate(SecuredEntityType.COHORT_DEFINITION, id)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to update cohort definition %d.", id));
        }
        var savedCohortDefinition = cohortDefinitionService.saveCohortDefinition(id, def);
        var dto = conversionService.convert(savedCohortDefinition, CohortDTO.class);
        authorizationManager.fillPermissions(savedCohortDefinition, dto);
        return dto;
    }

    /**
     * Queues up a generate cohort task for the specified cohort definition id.
     *
     * @param id
     *         the Cohort Definition ID to generate
     * @param sourceKey
     *         The source to execute the cohort generation
     * @return the job info for the cohort generation
     */
    @GetMapping("/{id}/generate/{sourceKey}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public JobExecutionResource generateCohort(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") final String sourceKey) {
        if (!authorizationManager.currentUserCanUpdate(SecuredEntityType.COHORT_DEFINITION, id)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to update cohort definition %d.", id));
        }

        if (!authorizationManager.currentUserCanRead(sourceKey)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to read source %s.", sourceKey));
        }

        return cohortGenerationService.generateCohortViaJob(id, sourceKey);
    }

    /**
     * Cancel a cohort generation task
     * <p>
     * This method updates the generation info to 'Canceled' and invalidates the job execution on the server.
     * <p>
     * Note: invalidating the job is performed by indicating that the job execution should stop at the next SQL step in
     * the analysis query. This means that the execution will not physically cancel until the current step in the SQL
     * query completes.
     *
     * @param id
     *         the id of the cohort definition being generated
     * @param sourceKey
     *         the sourceKey for the target database for generation
     * @return
     */
    @GetMapping("/{id}/cancel/{sourceKey}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Response cancelGenerateCohort(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") final String sourceKey) {
        if (!authorizationManager.currentUserCanUpdate(SecuredEntityType.COHORT_DEFINITION, id)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to update cohort definition %d.", id));
        }

        if (!authorizationManager.currentUserCanRead(sourceKey)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to read source %s.", sourceKey));
        }
        cohortGenerationService.cancelGenerateCohort(id, sourceKey);
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Returns a list of cohort generation info objects.
     * <p>
     * Cohort generation info objects refers to the information related to the generation on a
     * source. This includes information about the starting time, duration, and execution status.
     * This method returns the generation information for any source the user has access to.
     *
     * @param id
     *         - the Cohort Definition ID to generate
     * @return information about the Cohort Analysis Job for each source
     */
    @GetMapping("/{id}/info")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    @Transactional
    public List<CohortGenerationInfoDTO> getInfo(@PathVariable("id") final int id) {
        checkReadPermission(id);
        var info = cohortDefinitionService.getInfo(id);
        return info.stream()
                .map(CohortGenerationInfoToCohortGenerationInfoDTOConverter::convert)
                .collect(Collectors.toList());
    }

    /**
     * Copies the specified cohort definition.
     * <p>
     * This method takes a cohort definition id, and creates a copy. This copy has no tags and the owner is set to the
     * user who made the copy.
     *
     * @param id
     *         - the Cohort Definition ID to copy
     * @return the copied cohort definition as a CohortDTO
     */
    @GetMapping("/{id}/copy")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.CREATE_COHORT_DEFINITION)")
    @Transactional
    public CohortDTO copy(@PathVariable("id") final int id) {
        checkReadPermission(id);
        var newCohortDefinition = cohortDefinitionService.copyCohortDefinition(id);
        var createdDto = conversionService.convert(newCohortDefinition, CohortDTO.class);
        authorizationManager.fillPermissions(newCohortDefinition, createdDto);
        return createdDto;
    }

    /**
     * Deletes the specified cohort definition
     * <p>
     * When a cohort definition is deleted, any generation job is canceled, and any generated sample is removed from the
     * sources.
     *
     * @param id
     *         - the Cohort Definition ID to delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.DELETE_COHORT_DEFINITION)")
    public void delete(@PathVariable("id") final int id) {
        if (!authorizationManager.currentUserCanDelete(SecuredEntityType.COHORT_DEFINITION, id)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to delete cohort definition %d.", id));
        }
        cohortDefinitionService.deleteCohortDefinition(id);
    }

    /**
     * Return concept sets used in a cohort definition as a zip file.
     * <p>
     * This method extracts the concept set from the specified cohort definition and serializes the elements as a CSV
     * and zips the results into a file with from "cohortdefinition_{id}_export.zip".
     *
     * @param id
     *         a cohort definition id
     * @return a binary stream containing the zip file with concept sets.
     */
    @GetMapping("/{id}/export/conceptset")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    public ResponseEntity<ByteArrayResource> exportConceptSets(@PathVariable("id") final int id) {
        checkReadPermission(id);
        var source = sourceService.getPriorityVocabularySource();
        if (Objects.isNull(source)) {
            throw new NotFoundException("There is no priority vocabulary source configured.");
        }

        var def = cohortDefinitionService.getCohortDefinitionWithDetail(id);
        if (Objects.isNull(def)) {
            throw new NotFoundException(String.format("There is no cohort definition with id = %d.", id));
        }

        var exports = cohortDefinitionService.getConceptSetExports(def, new SourceInfo(source));
        var exportStream = ExportUtil.writeConceptSetExportToCSVAndZip(exports);
        return HttpUtils.respondBinary(exportStream, String.format("cohortdefinition_%d_export.zip", def.getId()));
    }

    /**
     * Get the Inclusion Rule report for the specified source and mode
     * <p>
     * The mode refers to the results for either 'all events' (0) or 'best event' (1). The best event report limits the
     * selected entry event to one-per-person, and therefore this result can be considered a 'by person' report.
     *
     * @param id
     *         a cohort definition id
     * @param sourceKey
     *         the source to fetch results from
     * @param modeId
     *         the mode of the report: 0 = all events, 1 = best event
     * @return a binary stream containing the zip file with concept sets.
     */
    @GetMapping("/{id}/report/{sourceKey}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    @Transactional
    public InclusionRuleReport getInclusionRuleReport(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") final String sourceKey,
            @RequestParam(name="mode", defaultValue="0") int modeId
    ) {
        checkReadPermission(id);
        if (!authorizationManager.currentUserCanRead(sourceKey)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to read source %s.", sourceKey));
        }
        var report = cohortDefinitionService.getInclusionRuleReport(id, sourceKey, modeId);
        return report;
    }

    /**
     * Checks the cohort definition for logic issues
     * <p>
     * This method runs a series of logical checks on a cohort definition and returns the set of warning, info and error
     * messages.
     *
     * @param expression
     *         The cohort definition expression
     * @return The cohort check result
     */
    @PostMapping("/check")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    @Transactional
    public CheckResultDTO runDiagnostics(@RequestBody CohortExpression expression) {
        Checker checker = new Checker();
        return new CheckResultDTO(checker.check(expression));
    }

    /**
     * Checks the cohort definition for logic issues
     * <p>
     * This method runs a series of logical checks on a cohort definition and returns the set of warning, info and error
     * messages.
     * <p>
     * This method is similar to /check except this method accepts a ChortDTO which includes tags.
     *
     * @param cohortDTO
     *         The cohort definition expression
     * @return The cohort check result
     */
    @PostMapping("/checkV2")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    @Transactional
    public CheckResult runDiagnosticsWithTags(@RequestBody CohortDTO cohortDTO) {
        var checker = new Checker();
        var checkResultDTO = new CheckResultDTO(checker.check(cohortDTO.getExpression()));
        var circeWarnings = checkResultDTO.getWarnings().stream()
                .map(WarningUtils::convertCirceWarning)
                .toList();
        var checkResult = new CheckResult(cohortChecker.check(cohortDTO));
        checkResult.getWarnings().addAll(circeWarnings);
        return checkResult;
    }

    /**
     * Render a cohort expression in html or markdown form.
     * <p>
     * This method calls out to the markdown renderer for CIRCE cohort expressions, and then converts to HTML if
     * required. The response will contain the media type as TEXT_PLAIN or markdown, or TEXT_HTML for html. The body of
     * the response is the print friendly content.
     *
     * @param expression
     *         The CIRCE cohort expression to render
     * @param format
     *         The format to render, can be either 'html' or 'markdown'.  Defaults to 'html'
     * @return an HTTP response with the content, with the appropriate MediaType based on the format that was
     *         requested.
     */
    @PostMapping("/printfriendly/cohort")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    public Response cohortPrintFriendly(
            @RequestBody CohortExpression expression,
            @RequestParam(name="format", defaultValue="html") String format) {
        var markdown = markdownPF.renderCohort(expression);
        return printFriendly(markdown, format);
    }

    /**
     * Render a list of concept sets in html or markdown form.
     * <p>
     * This method calls out to the markdown renderer concept set expressions, and then converts to HTML if required.
     * The response will contain the media type as TEXT_PLAIN or markdown, or TEXT_HTML for html. The body of the
     * response is the print friendly content.
     *
     * @param conceptSetList
     *         A List of concept set expressions
     * @param format
     *         The format to render, can be either 'html' or 'markdown'.  Defaults to 'html'
     * @return an HTTP response with the content, with the appropriate MediaType based on the format that was
     *         requested.
     */
    @PostMapping("/printfriendly/conceptsets")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    public Response conceptSetListPrintFriendly(
            @RequestBody List<ConceptSet> conceptSetList,
            @RequestParam(name="format", defaultValue="html") String format) {
        var markdown = markdownPF.renderConceptSetList(conceptSetList.toArray(new ConceptSet[0]));
        return printFriendly(markdown, format);
    }

    private Response printFriendly(String markdown, String format) {
        ResponseBuilder res;
        if ("html".equalsIgnoreCase(format)) {
            var parser = Parser.builder().extensions(extensions).build();
            var document = parser.parse(markdown);
            var renderer = HtmlRenderer.builder().extensions(extensions).build();
            var html = renderer.render(document);
            res = Response.ok(html, MediaType.TEXT_HTML);
        } else if ("markdown".equals(format)) {
            res = Response.ok(markdown, MediaType.TEXT_PLAIN);
        } else {
            res = Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE);
        }
        return res.build();
    }

    /**
     * Assign tag to Cohort Definition
     *
     * @param id
     *         the cohort definition id
     * @param tagId
     *         the tag id
     */
    @PostMapping("/{id}/tag")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_COHORT_DEFINITION)")
    @Transactional
    public void assignTag(@PathVariable final Integer id, @RequestBody final int tagId) {
        if (!authorizationManager.currentUserCanUpdate(SecuredEntityType.COHORT_DEFINITION, id)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to update cohort definition %d.", id));
        }
        cohortDefinitionService.assignTag(id, tagId);
    }

    /**
     * Unassign tag from Cohort Definition
     *
     * @param id
     *         the cohort definition id
     * @param tagId
     *         the tag id
     */
    @DeleteMapping("/{id}/tag/{tagId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_COHORT_DEFINITION)")
    @Transactional
    public void unassignTag(
            @PathVariable final Integer id,
            @PathVariable final int tagId
    ) {
        if (!authorizationManager.currentUserCanUpdate(SecuredEntityType.COHORT_DEFINITION, id)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to update cohort definition %d.", id));
        }
        cohortDefinitionService.unassignTag(id, tagId);
    }

    /**
     * Assign protected tag to Cohort Definition. This method passes through to assignTag(), but permissions to access
     * this endpoint is determined by the path /{id}/protectedtag
     *
     * @param id
     * @param tagId
     */
    @PostMapping("/{id}/protectedtag")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_COHORT_DEFINITION)")
    @Transactional
    public void assignPermissionProtectedTag(@PathVariable final int id, @RequestBody final int tagId) {
        if (!authorizationManager.currentUserCanUpdate(SecuredEntityType.COHORT_DEFINITION, id)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to update cohort definition %d.", id));
        }
        cohortDefinitionService.assignTag(id, tagId);
    }

    /**
     * Unassign protected tag from Cohort Definition
     *
     * @param id
     * @param tagId
     */
    @DeleteMapping("/{id}/protectedtag/{tagId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_COHORT_DEFINITION)")
    @Transactional
    public void unassignPermissionProtectedTag(
            @PathVariable final int id,
            @PathVariable final int tagId
    ) {
        if (!authorizationManager.currentUserCanUpdate(SecuredEntityType.COHORT_DEFINITION, id)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to update cohort definition %d.", id));
        }
        cohortDefinitionService.unassignTag(id, tagId);
    }

    /**
     * Get list of versions of Cohort Definition
     *
     * @param id
     *         the cohort definition id
     * @return the list of VersionDTO containing version info for the cohort definition
     */
    @GetMapping("/{id}/version")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    @Transactional
    public List<VersionDTO> getVersions(@PathVariable("id") final int id) {
        checkReadPermission(id);
        var versions = this.versionService.getVersions(id);
        return versions.stream()
                .map(VersionBaseToVersionDTOConverter::convert)
                .collect(Collectors.toList());
    }

    /**
     * Get version of Cohort Definition
     *
     * @param id
     *         The cohort definition id
     * @param version
     *         The version to fetch
     * @return
     */
    @GetMapping("/{id}/version/{version}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    @Transactional
    public CohortVersionFullDTO getVersion(
            @PathVariable("id") final int id,
            @PathVariable("version") final int version) {
        checkReadPermission(id);
        checkVersion(id, version, false);
        CohortVersion cohortVersion = versionService.getById(id, version);

        return cohortVersionToCohortVersionFullDTOConverter.convert(cohortVersion);
    }

    /**
     * Updates version of Cohort Definition.
     * <p>
     * The specified version is checked for permission and if it exists, and if this check passes, the version is
     * updated.
     *
     * @param id
     *         The cohort definition id
     * @param version
     *         the id of the version
     * @param updateDTO
     *         the new version data
     * @return the updated version state as VersionDTO
     */
    @PutMapping("/{id}/version/{version}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_COHORT_DEFINITION)")
    @Transactional
    public VersionDTO updateVersion(
            @PathVariable("id") final int id,
            @PathVariable("version") final int version,
            @RequestBody VersionUpdateDTO updateDTO) {
        if (!authorizationManager.currentUserCanUpdate(SecuredEntityType.COHORT_DEFINITION, id)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to update cohort definition %d.", id));
        }
        checkVersion(id, version);
        updateDTO.setAssetId(id);
        updateDTO.setVersion(version);
        CohortVersion updated = versionService.update(updateDTO);

        return VersionToVersionDTOConverter.convert(updated);
    }

    /**
     * Delete version of Cohort Definition
     *
     * @param id
     *         the cohort definition id
     * @param version
     *         the version id
     */
    @DeleteMapping("/{id}/version/{version}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_COHORT_DEFINITION)")
    @Transactional
    public void deleteVersion(
            @PathVariable("id") final int id,
            @PathVariable("version") final int version) {
        if (!authorizationManager.currentUserCanUpdate(SecuredEntityType.COHORT_DEFINITION, id)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to update cohort definition %d.", id));
        }
        checkVersion(id, version);
        versionService.delete(id, version);
    }

    /**
     * Create a new asset from version of Cohort Definition.
     * <p>
     * This method fetches the cohort definition version based on the id and version parameter, and creates a new cohort
     * definition (without tags) and an automatically generated name.
     *
     * @param id
     *         the cohort definition id
     * @param version
     *         the version id
     * @return
     */
    @PutMapping("/{id}/version/{version}/createAsset")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.CREATE_COHORT_DEFINITION) and @authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    @Transactional
    public CohortDTO copyAssetFromVersion(
            @PathVariable("id") final int id,
            @PathVariable("version") final int version) {
        checkReadPermission(id);
        checkVersion(id, version, false);
        var cohortVersion = versionService.getById(id, version);
        var fullDTO = this.cohortVersionToCohortVersionFullDTOConverter.convert(cohortVersion);
        var dto =  CohortRawDTOToCohortDTOConverter.convert(fullDTO.getEntityDTO());
        dto.setId(null);
        dto.setTags(null);
        dto.setName(NameUtils.getNameForCopy(dto.getName(), cohortDefinitionService::getNamesLike,
                cohortDefinitionService.findByName(dto.getName())));
        return createCohortDefinition(dto);
    }

    /**
     * Get list of cohort definitions with assigned tags.
     * <p>
     * This method accepts a TagNameListRequestDTO that contains the list of tag names to find cohort definitions with.
     *
     * @param requestDTO
     *         contains a list of tag names
     * @return the list of cohort definitions that match one of the included tag names.
     */
    @PostMapping("/byTags")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_COHORT_DEFINITION)")
    @Transactional
    public List<CohortDTO> listByTags(@RequestBody TagNameListRequestDTO requestDTO) {
        return cohortDefinitionService.listByTags(requestDTO)
            .stream()
            .filter(!webApiProperties.security().defaultGlobalReadPermissions()
                    ? authorizationManager::currentUserCanRead
                    : entity -> true)
            .map(entity -> {
                var dto = conversionService.convert(entity, CohortDTO.class);
                authorizationManager.fillPermissions(entity, dto);
                return dto;
            })
            .collect(Collectors.toList());
    }

    private void checkReadPermission(int id) {
        if (this.webApiProperties.security().defaultGlobalReadPermissions()) {
            return;
        }
        if (!authorizationManager.currentUserCanRead(SecuredEntityType.COHORT_DEFINITION, id)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to read cohort definition %d.", id));
        }
    }

    private void checkVersion(int id, int version) {
        checkVersion(id, version, true);
    }

    private void checkVersion(int id, int version, boolean checkOwnerShip) {
        var cohortVersion = versionService.getById(id, version);
        if (cohortVersion == null) {
            throw new NotFoundException(String.format("There is no cohort definition version with id = %d.", version));
        }

        var entity = cohortDefinitionService.getCohortDefinition(id);
        if (entity == null) {
            throw new NotFoundException(String.format("There is no cohort definition with id = %d.", id));
        }
        if (checkOwnerShip) {
            authorizationManager.throwIfCurrentUserDoesntOwn(entity);
        }
    }
}
