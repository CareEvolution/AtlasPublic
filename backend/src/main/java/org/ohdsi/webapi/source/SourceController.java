package org.ohdsi.webapi.source;

import jakarta.ws.rs.core.Response;

import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.Permissions;
import org.ohdsi.webapi.vocabulary.VocabularyService;
import org.springframework.core.convert.ConversionService;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

// Note: Permissions for source access are a little different than other entities. A user with UPDATE_SOURCE
// will be able to read and update all sources. The UPDATE_SOURCE permissions should be granted only to
// administrators. Normal users will have source:<sourceKey>:read permissions assigned to them for each source
// they should have access to.

/**
 * REST API for working with data sources (CDMs). This controller provides endpoints for retrieving source 
 * metadata, creating and updating sources, and checking source connections. The source metadata includes 
 * information about the source's name, key, SQL dialect, and daimon types. The controller also provides 
 * endpoints for refreshing the cached source data and setting the priority of daimon types for a source.
 */
@RestController
@RequestMapping("/source")
public class SourceController {
    public static final String SECURE_MODE_ERROR = "This feature requires the administrator to enable security for the application";

    private final SourceService sourceService;
    private final VocabularyService vocabularyService;
    private final ConversionService conversionService;
    private final AuthorizationManager authorizationManager;

    public SourceController(
        SourceService sourceService,
        VocabularyService vocabularyService,
        ConversionService conversionService,
        AuthorizationManager authorizationManager
    ) {
        this.sourceService = sourceService;
        this.vocabularyService = vocabularyService;
        this.conversionService = conversionService;
        this.authorizationManager = authorizationManager;
    }

    /**
     * Get the list of supported SQL dialects for sources.
     * @return
     * A list of DatabaseTypeDto objects representing the supported SQL dialects.
     */
    @GetMapping("/supported-dialects")
    public List<DatabaseTypeDto> getSupportedDialects() {
        return sourceService.getSupportedDialects();
    }

    /**
     * Gets the list of all Sources in WebAPI database. Sources with a non-null deleted_date are not returned (ie: these
     * are soft deleted)
     *
     * @return A list of all CDM sources with the ID, name, SQL dialect, and key for each source. The {sourceKey} is
     *         used in other WebAPI endpoints to identify CDMs.
     */
    @GetMapping("/sources")
    public Collection<SourceInfo> getSources() {
        return sourceService.getSources()
                .stream()
                .filter((source) -> authorizationManager.currentUserCanRead(source.getSourceKey())
                        || authorizationManager.currentUserHasPermission(Permissions.UPDATE_SOURCE))
                .map(this::convertSource)
                .collect(Collectors.toList());
    }

    /**
     * Refresh cached CDM database metadata
     *
     * @return A list of all CDM sources with the ID, name, SQL dialect, and key for each source (same as the 'sources'
     *         endpoint) after refreshing the cached sourced data.
     */
    @GetMapping("/refresh")
    public Collection<SourceInfo> refreshSources() {
        // Anyone can refresh the source cache
        sourceService.invalidateCache();
        vocabularyService.clearVocabularyInfoCache();
        return getSources();
    }

    /**
     * Get the priority vocabulary source.
     * <p>
     * WebAPI designates one CDM vocabulary as the priority vocabulary to be used for vocabulary searches in Atlas.
     *
     * @return The CDM metadata for the priority vocabulary.
     */
    @GetMapping("/priorityVocabulary")
    public SourceInfo getPriorityVocabularySourceInfo() {
        // This will return the highest priority vocabulary source that the user has access to.
        var source = sourceService.getPriorityVocabularySource();
        return source == null ? null : convertSource(source);
    }

    /**
     * Get source by key
     *
     * @param sourceKey
     * @return Metadata for a single Source that matches the <code>sourceKey</code>.
     */
    @GetMapping("/{key}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public SourceInfo getSource(@PathVariable("key") final String sourceKey) {
        checkUserCanAccessSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        return convertSource(source);
    }

    /**
     * Get Source Details
     * <p>
     * Source Details contains connection-specific information like JDBC url and authentication information.
     *
     * @param sourceId
     * @return
     */
    @GetMapping("/details/{sourceId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public SourceDetails getSourceDetails(@PathVariable("sourceId") Integer sourceId) {
        var source = sourceService.findBySourceId(sourceId);
        checkUserCanAccessSource(source.getSourceKey());
        return convertSourceDetails(source);
    }

    /**
     * Create a Source
     *
     * @param file
     *         the keyfile
     * @param request
     *         contains the source information (name, key, etc)
     * @return a new SourceInfo for the created source
     * @throws Exception
     */
    @PostMapping(value = "")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.CREATE_SOURCE)")
    public SourceInfo createSource(
            @RequestParam(name = "source") MultipartFile request,
            @RequestParam(name = "keyfile", required = false) MultipartFile file
    ) throws Exception {
        var requestString = new String(request.getBytes(), StandardCharsets.UTF_8);
        var objectMapper = new ObjectMapper();
        var sourceRequest = objectMapper.readValue(requestString, SourceRequest.class);
        var source = SourceRequestToSourceEntityConverter.convert(sourceRequest);
        var saved = sourceService.createSource(source, file);
        var sourceInfo = convertSource(saved);
        return sourceInfo;
    }

    /**
     * Create a Source without a keyfile
     *
     * @param sourceRequest
     *         contains the source information (name, key, etc)
     * @return a new SourceInfo for the created source
     * @throws Exception
     */
    @PostMapping(value = "/no-keyfile")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.CREATE_SOURCE)")
    public SourceInfo createSourceNoKeyFile(
            @RequestBody() SourceRequest sourceRequest) throws Exception {
        var source = SourceRequestToSourceEntityConverter.convert(sourceRequest);
        var saved = sourceService.createSource(source, null);
        var sourceInfo = convertSource(saved);
        return sourceInfo;
    }

    /**
     * Updates a Source with the provided details from multiple files
     *
     * @param sourceId
     *         the ID of the source to update
     * @param file
     *         the keyfile
     * @param request
     *         contains the source information (name, key, etc)
     * @return the updated SourceInfo for the source
     * @throws IOException
     */
    @PostMapping("/{sourceId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_SOURCE)")
    public SourceInfo updateSource(
            @PathVariable("sourceId") Integer sourceId,
            @RequestParam("source") MultipartFile request,
            @RequestParam(name = "keyfile", required = false) MultipartFile file
    ) throws IOException {
        var requestString = new String(request.getBytes(), StandardCharsets.UTF_8);
        var objectMapper = new ObjectMapper();
        var sourceRequest = objectMapper.readValue(requestString, SourceRequest.class);
        var updated = SourceRequestToSourceEntityConverter.convert(sourceRequest);
        var result = sourceService.updateSource(sourceId, updated, sourceRequest.isCheckConnection(), file);
        return convertSource(result);
    }


    /**
     * Delete a source.
     *
     * @param sourceId
     *        the ID of the source to delete
     * @return ok response if the source was deleted successfully
     * @throws Exception
     */
    @DeleteMapping("/{sourceId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.DELETE_SOURCE)")
    public Response delete(@PathVariable("sourceId") Integer sourceId) throws Exception {
        sourceService.delete(sourceId);
        return Response.ok().build();
    }

    /**
     * Check source connection.
     * <p>
     * This method attempts to connect to the source by calling 'select 1' on the source connection.
     *
     * @param sourceKey
     * @return the source information if the connection is successful
     */
    @GetMapping("/connection/{key}")
    @Transactional(noRollbackFor = CannotGetJdbcConnectionException.class)
    public SourceInfo checkConnection(@PathVariable("key") final String sourceKey) {
        checkUserCanAccessSource(sourceKey);
        final var source = sourceService.findBySourceKey(sourceKey);
        sourceService.checkConnection(source);
        return convertSource(source);
    }

    /**
     * Get the first daimon (and associated source) that has priority. In the event of a tie, the first source searched
     * wins.
     *
     * @return A map of the form: {daimonType -> sourceInfo} for each daimon type that has a source with priority > 0. The sourceInfo contains the metadata for the source with the highest priority for that daimon type.
     */
    @GetMapping("/daimon/priority")
    public Map<SourceDaimonEntity.DaimonType, SourceInfo> getPriorityDaimons() {
        // This returns the source for each daimon type that has the highest priority
        // The service ensures that the user has permission to read each source
        return sourceService
                .getPriorityDaimons()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> convertSource(e.getValue())));
    }

    /**
     * Set priority of daimon
     * <p>
     * Set the priority of the specified daimon of the specified source, and set the other daimons to 0.
     *
     * @param sourceKey
     * @param daimonTypeName
     * @return ok response if the priority was updated successfully
     */

    @PostMapping("/{sourceKey}/daimons/{daimonType}/set-priority")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_SOURCE)")
    public Response updateSourcePriority(
            @PathVariable("sourceKey") final String sourceKey,
            @PathVariable("daimonType") final String daimonTypeName) {
        // This will potentially modify the daimon priority for multiple sources. The user has to have access to
        // update sources for the daimon type in order to perform this action.
        sourceService.updateSourcePriority(sourceKey, daimonTypeName);
        return Response.ok().build();
    }

    private void checkUserCanAccessSource(String sourceKey) {
        if (!authorizationManager.currentUserCanRead(sourceKey)
                && !authorizationManager.currentUserHasPermission(Permissions.UPDATE_SOURCE)) {
            throw new AccessDeniedException("User does not have permission to access this source");
        }
    }

    private SourceInfo convertSource(SourceEntity source) {
        var sourceInfo = conversionService.convert(source, SourceInfo.class);
        if (sourceInfo == null) {
            return null;
        }
        sourceInfo.setHasWriteAccess(authorizationManager.currentUserHasPermission(Permissions.UPDATE_SOURCE));
        sourceInfo.setHasDeleteAccess(authorizationManager.currentUserHasPermission(Permissions.DELETE_SOURCE));
        return sourceInfo;
    }

    private SourceDetails convertSourceDetails(SourceEntity source) {
        var sourceDetails = conversionService.convert(source, SourceDetails.class);
        if (sourceDetails == null) {
            return null;
        }
        sourceDetails.setHasWriteAccess(authorizationManager.currentUserHasPermission(Permissions.UPDATE_SOURCE));
        sourceDetails.setHasDeleteAccess(authorizationManager.currentUserHasPermission(Permissions.DELETE_SOURCE));
        return sourceDetails;
    }
}
