/*
 * Copyright 2015 fdefalco.
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
package org.ohdsi.webapi.conceptset;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.ohdsi.circe.vocabulary.ConceptSetExpression;
import org.ohdsi.webapi.check.CheckResult;
import org.ohdsi.webapi.check.checker.conceptset.ConceptSetChecker;
import org.ohdsi.webapi.conceptset.dto.ConceptSetDTO;
import org.ohdsi.webapi.conceptset.dto.ConceptSetVersionFullDTO;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.model.SecuredEntityType;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.tag.dto.TagNameListRequestDTO;
import org.ohdsi.webapi.util.ExportUtil;
import org.ohdsi.webapi.util.NameUtils;
import org.ohdsi.webapi.versioning.VersionBaseToVersionDTOConverter;
import org.ohdsi.webapi.versioning.VersionToVersionDTOConverter;
import org.ohdsi.webapi.versioning.dto.VersionDTO;
import org.ohdsi.webapi.versioning.dto.VersionUpdateDTO;
import org.ohdsi.webapi.versioning.service.ConceptSetVersionService;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for working with concept sets.
 *
 */
@RestController
@RequestMapping("/conceptset")
@Transactional
public class ConceptSetController {

    private AuthorizationManager authorizationManager;
    private final ConceptSetService conceptSetService;
    private final ConceptSetChecker checker;
    private final ConceptSetGenerationInfoRepository conceptSetGenerationInfoRepository;
    private final SourceService sourceService;
    private final ConceptSetVersionService versionService;
    private final WebApiProperties webApiProperties;
    private final ConversionService conversionService;

    public static final String COPY_NAME = "copyName";

    public ConceptSetController(
            AuthorizationManager authorizationManager,
            ConceptSetService conceptSetService,
            ConceptSetChecker checker,
            ConceptSetGenerationInfoRepository conceptSetGenerationInfoRepository,
            SourceService sourceService,
            ConceptSetVersionService versionService,
            WebApiProperties webApiProperties,
            ConversionService conversionService) {
        this.authorizationManager = authorizationManager;
        this.conceptSetService = conceptSetService;
        this.checker = checker;
        this.conceptSetGenerationInfoRepository = conceptSetGenerationInfoRepository;
        this.sourceService = sourceService;
        this.versionService = versionService;
        this.webApiProperties = webApiProperties;
        this.conversionService = conversionService;
    }

    /**
     * Get the concept set based in the identifier
     *
     * @param id
     *         The concept set ID
     * @return The concept set definition
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    public ConceptSetDTO getConceptSet(@PathVariable("id") final int id) {
        checkReadPermission(id);
        var conceptSet = conceptSetService.getConceptSet(id);
        return conversionService.convert(conceptSet, ConceptSetDTO.class);
    }

    /**
     * Get the full list of concept sets in the WebAPI database
     *
     * @return A list of all concept sets in the WebAPI database
     */
    @GetMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    public Collection<ConceptSetDTO> getConceptSets() {
        return conceptSetService.getAllConceptSets().stream()
            .filter(!webApiProperties.security().defaultGlobalReadPermissions()
                    ? authorizationManager::currentUserCanRead
                    : entity -> true)
            .map(conceptSet -> {
                return conversionService.convert(conceptSet, ConceptSetDTO.class);
            })
            .collect(Collectors.toList());
    }

    /**
     * Get the concept set items for a selected concept set ID.
     *
     * @param id
     *         The concept set identifier
     * @return A list of concept set items
     */
    @GetMapping("/{id}/items")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    public Iterable<ConceptSetItem> getConceptSetItems(@PathVariable("id") final int id) {
        checkReadPermission(id);
        return conceptSetService.getConceptSetItems(id);
    }

    /**
     * Get the concept set expression for a selected version of the expression
     *
     * @param id
     *         The concept set ID
     * @param version
     *         The version identifier
     * @return The concept set expression
     */
    @GetMapping("/{id}/version/{version}/expression")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    public ConceptSetExpression getConceptSetExpression(
            @PathVariable("id") final int id,
            @PathVariable("version") final int version) {
        checkVersion(id, version);
        var source = sourceService.getPriorityVocabularySource();
        if (source == null) {
            throw new AccessDeniedException("You do not have access to the priority vocabulary source.");
        }
        return conceptSetService.getConceptSetExpression(id, version, source);
    }

    /**
     * Get the concept set expression by version for the selected source key. NOTE: This method requires the
     * specification of a source key but it does not appear to be used by the underlying code.
     *
     * @param id
     *         The concept set identifier
     * @param version
     *         The version of the concept set
     * @param sourceKey
     *         The source key
     * @return The concept set expression for the selected version
     */
    @GetMapping("/{id}/version/{version}/expression/{sourceKey}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    public ConceptSetExpression getConceptSetExpression(
            @PathVariable("id") final int id,
            @PathVariable("version") final int version,
            @PathVariable("sourceKey") final String sourceKey) {
        checkVersion(id, version);
        if (!authorizationManager.currentUserCanRead(sourceKey)) {
            throw new AccessDeniedException("You do not have permission to read from this source");
        }
        var source = sourceService.findBySourceKey(sourceKey);
        return conceptSetService.getConceptSetExpression(id, version, source);
    }

    /**
     * Get the concept set expression by identifier
     *
     * @param id
     *         The concept set identifier
     * @return The concept set expression
     */
    @GetMapping("/{id}/expression")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    public ConceptSetExpression getConceptSetExpression(@PathVariable("id") final int id) {
        checkReadPermission(id);
        var source = sourceService.getPriorityVocabularySource();
        if (source == null) {
            throw new AccessDeniedException("You do not have access to the priority vocabulary source.");
        }
        return conceptSetService.getConceptSetExpression(id, null, source);
    }

    /**
     * Get the concept set expression by identifier and source key
     *
     * @param id
     *         The concept set ID
     * @param sourceKey
     *         The source key
     * @return The concept set expression
     */
    @GetMapping("/{id}/expression/{sourceKey}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public ConceptSetExpression getConceptSetExpression(
            @PathVariable("id") final int id,
            @PathVariable("sourceKey") final String sourceKey) {
        checkReadPermission(id);
        if (!authorizationManager.currentUserCanRead(sourceKey)) {
            throw new AccessDeniedException("You do not have permission to read from this source");
        }
        var source = sourceService.findBySourceKey(sourceKey);
        return conceptSetService.getConceptSetExpression(id, null, source);
    }

    /**
     * Check if a concept set with the same name exists in the WebAPI database. The name is checked against the selected
     * concept set ID to ensure that only the selected concept set ID has the name specified.
     *
     * @param id
     *         The concept set ID
     * @param name
     *         The name of the concept set
     * @return The count of concept sets with the name, excluding the specified concept set ID.
     */
    @GetMapping("/{id}/exists")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    public int getCountCSetWithSameName(
            @PathVariable("id") final int id,
            @RequestParam(name="name") String name) {
        return conceptSetService.getCountOfConceptSetsWithSameName(id, name);
    }

    /**
     * Update the concept set items for the selected concept set ID in the WebAPI database.
     * <p>
     * The concept set has two parts: 1) the elements of the ConceptSetDTO that consist of the identifier, name, etc. 2)
     * the concept set items which contain the concepts and their mapping (i.e. include descendants).
     *
     * @param id
     *         The concept set ID
     * @param items
     *         An array of ConceptSetItems
     * @return Boolean: true if the save is successful
     */
    @PutMapping("/{id}/items")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_CONCEPT_SET)")
    public boolean saveConceptSetItems(@PathVariable("id") final int id, @RequestBody ConceptSetItem[] items) {
        checkUpdatePermission(id);
        return conceptSetService.saveConceptSetItems(id, items);
    }

    /**
     * Exports a list of concept sets, based on the conceptSetList argument, to one or more comma separated value (CSV)
     * file(s), compresses the files into a ZIP file and sends the ZIP file to the client.
     *
     * @param conceptSetList
     *         A list of concept set identifiers in the format
     *         conceptset=concept_set_id_1+concept_set_id_2+concept_set_id_n
     * @return
     * @throws Exception
     */
    @GetMapping("/exportlist")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Response exportConceptSetList(@RequestParam("conceptsets") final String conceptSetList) throws Exception {
        var conceptSetIds = new ArrayList<Integer>();
        var conceptSetItems = conceptSetList.split("\\+");
        for (var csi : conceptSetItems) {
            conceptSetIds.add(Integer.valueOf(csi));
        }
        if (conceptSetIds.size() <= 0) {
            throw new IllegalArgumentException(
                    "You must supply a querystring value for conceptsets that is of the form: ?conceptset=<concept_set_id_1>+<concept_set_id_2>+<concept_set_id_n>");
        }

        for (var conceptSetId : conceptSetIds) {
            checkReadPermission(conceptSetId);
        }
        var source = sourceService.getPriorityVocabularySource();
        if (source == null) {
            throw new AccessDeniedException("You do not have access to the priority vocabulary source.");
        }

        ByteArrayOutputStream baos;
        var cs = new ArrayList<ConceptSetExport>();
        Response response = null;
        try {
            // Load all of the concept sets requested
            for (int i = 0; i < conceptSetIds.size(); i++) {
                // Get the concept set information
                cs.add(conceptSetService.getConceptSetForExport(conceptSetIds.get(i), source));
            }
            // Write Concept Set Expression to a CSV
            baos = ExportUtil.writeConceptSetExportToCSVAndZip(cs);

            response = Response
                    .ok(baos.toByteArray())
                    .type(MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"conceptSetExport.zip\"")
                    .build();

        } catch (Exception ex) {
            throw ex;
        }
        return response;
    }

    /**
     * Exports a single concept set to a comma separated value (CSV) file, compresses to a ZIP file and sends to the
     * client.
     *
     * @param id
     *         The concept set ID
     * @return A zip file containing the exported concept set
     * @throws Exception
     */
    @GetMapping("/{id}/export")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Response exportConceptSetToCSV(@PathVariable("id") final String id) throws Exception {
        return this.exportConceptSetList(id);
    }

    /**
     * Save a new concept set to the WebAPI database
     *
     * @param conceptSetDTO
     *         The concept set to save
     * @return The concept set saved with the concept set identifier
     */
    @PostMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.CREATE_CONCEPT_SET)")
    public ConceptSetDTO createConceptSet(@RequestBody ConceptSetDTO conceptSetDTO) {
        return conceptSetService.createConceptSet(conceptSetDTO);
    }

    /**
     * Creates a concept set name, based on the selected concept set ID, that is used when generating a copy of an
     * existing concept set. This function is generally used in conjunction with the copy endpoint to create a unique
     * name and then save a copy of an existing concept set.
     *
     * @param id
     *         The concept set ID
     * @return A map of the new concept set name and the existing concept set name
     */
    @GetMapping("/{id}/copy-name")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    public Map<String, String> getNameForCopy(@PathVariable("id") final int id) {
        checkReadPermission(id);
        var conceptSet = conceptSetService.getConceptSet(id);
        var name = NameUtils.getNameForCopy(conceptSet.getName(), conceptSetService::getNamesLike, Optional.of(conceptSet));
        return Collections.singletonMap(COPY_NAME, name);
    }

    /**
     * Updates the concept set for the selected concept set.
     * <p>
     * The concept set has two parts: 1) the elements of the ConceptSetDTO that consist of the identifier, name, etc. 2)
     * the concept set items which contain the concepts and their mapping (i.e. include descendants).
     *
     * @param id
     *         The concept set identifier
     * @param conceptSetDTO
     *         The concept set header
     * @return The
     * @throws Exception
     */
    @PutMapping("/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_CONCEPT_SET)")
    public ConceptSetDTO updateConceptSet(@PathVariable("id") final int id, @RequestBody ConceptSetDTO conceptSetDTO) throws Exception {
        checkUpdatePermission(id);
        return conceptSetService.updateConceptSet(id, conceptSetDTO);
    }

    /**
     * Get the concept set generation information for the selected concept set ID. This function only works with the
     * configuration of the CEM data source.
     *
     * @param id
     *         The concept set identifier.
     * @return A collection of concept set generation info objects
     * @see <a href="https://github.com/OHDSI/CommonEvidenceModel/wiki">Common Evidence Model Wiki</a>
     */
    @GetMapping("/{id}/generationinfo")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    public Collection<ConceptSetGenerationInfo> getConceptSetGenerationInfo(@PathVariable("id") final int id) {
        checkReadPermission(id);
        return this.conceptSetGenerationInfoRepository.findAllByConceptSetId(id);
    }

    /**
     * Delete the selected concept set by concept set identifier
     *
     * @param id
     *         The concept set ID
     */
    @Transactional(rollbackOn = Exception.class, dontRollbackOn = EmptyResultDataAccessException.class)
    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.DELETE_CONCEPT_SET)")
    public void deleteConceptSet(@PathVariable("id") final int id) {
        if (!authorizationManager.currentUserCanDelete(SecuredEntityType.CONCEPT_SET, id)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to delete Concept Set with id = %d", id));
        }
        conceptSetService.deleteConceptSet(id);
    }

    /**
     * Assign tag to Concept Set
     *
     * @param id
     *         The concept set ID
     * @param tagId
     *         The tag ID
     * @since v2.10.0
     */
    @PostMapping("/{id}/tag")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_CONCEPT_SET)")
    public void assignTag(@PathVariable("id") final Integer id, @RequestBody final int tagId) {
        checkUpdatePermission(id);
        conceptSetService.assignTag(id, tagId);
    }

    /**
     * Unassign tag from Concept Set
     *
     * @param id
     *         The concept set ID
     * @param tagId
     *         The tag ID
     * @since v2.10.0
     */
    @DeleteMapping("/{id}/tag/{tagId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_CONCEPT_SET)")
    public void unassignTag(@PathVariable("id") final Integer id, @PathVariable("tagId") final int tagId) {
        checkUpdatePermission(id);
        conceptSetService.unassignTag(id, tagId);
    }

    /**
     * Assign protected tag to Concept Set
     *
     * @param id
     *         The concept set ID
     * @param tagId
     *         The tag ID
     * @since v2.10.0
     */
    @PostMapping("/{id}/protectedtag")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_CONCEPT_SET)")
    public void assignPermissionProtectedTag(@PathVariable("id") final int id, final int tagId) {
        checkUpdatePermission(id);
        conceptSetService.assignTag(id, tagId);
    }

    /**
     * Unassign protected tag from Concept Set
     *
     * @param id
     *         The concept set ID
     * @param tagId
     *         The tag ID
     * @since v2.10.0
     */
    @DeleteMapping("/{id}/protectedtag/{tagId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_CONCEPT_SET)")
    public void unassignPermissionProtectedTag(@PathVariable("id") final int id, @PathVariable("tagId") final int tagId) {
        checkUpdatePermission(id);
        conceptSetService.unassignTag(id, tagId);
    }

    /**
     * Checks a concept set for diagnostic problems. At this time, this appears to be an endpoint used to check to see
     * which tags are applied to a concept set.
     *
     * @param conceptSetDTO
     *         The concept set
     * @return A check result
     * @since v2.10.0
     */
    @PostMapping("/check")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    public CheckResult runDiagnostics(@RequestBody ConceptSetDTO conceptSetDTO) {
        return new CheckResult(checker.check(conceptSetDTO));
    }

    /**
     * Get a list of versions of the selected concept set
     *
     * @param id
     *         The concept set ID
     * @return A list of version information
     * @since v2.10.0
     */
    @GetMapping("/{id}/version")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    public List<VersionDTO> getVersions(@PathVariable("id") final int id) {
        checkReadPermission(id);
        var versions = versionService.getVersions(id);
        return versions.stream()
                .map(VersionBaseToVersionDTOConverter::convert)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific version of a concept set
     *
     * @param id
     *         The concept set ID
     * @param version
     *         The version ID
     * @return The concept set for the selected version
     * @since v2.10.0
     */
    @GetMapping("/{id}/version/{version}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    public ConceptSetVersionFullDTO getVersion(
            @PathVariable("id") final int id,
            @PathVariable("version") final int version
    ) {
        checkVersion(id, version);
        var conceptSetVersion = versionService.getById(id, version);
        return conversionService.convert(conceptSetVersion, ConceptSetVersionFullDTO.class);
    }

    /**
     * Update a specific version of a selected concept set
     *
     * @param id
     *         The concept set ID
     * @param version
     *         The version ID
     * @param updateDTO
     *         The version update
     * @return The version information
     * @since v2.10.0
     */
    @PutMapping("/{id}/version/{version}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_CONCEPT_SET)")
    public VersionDTO updateVersion(@PathVariable("id") final int id, @PathVariable("version") final int version,
            @RequestBody VersionUpdateDTO updateDTO) {
        checkVersion(id, version);
        checkUpdatePermission(id);
        updateDTO.setAssetId(id);
        updateDTO.setVersion(version);
        var updated = versionService.update(updateDTO);
        return VersionToVersionDTOConverter.convert(updated);
    }

    /**
     * Delete a version of a concept set
     *
     * @param id
     *         The concept ID
     * @param version
     *         THe version ID
     * @since v2.10.0
     */
    @DeleteMapping("/{id}/version/{version}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_CONCEPT_SET)")
    public void deleteVersion(@PathVariable("id") final int id, @PathVariable("version") final int version) {
        checkVersion(id, version);
        checkUpdatePermission(id);
        versionService.delete(id, version);
    }

    /**
     * Create a new asset from a specific version of the selected concept set
     *
     * @param id
     *         The concept set ID
     * @param version
     *         The version ID
     * @return The concept set copy
     * @since v2.10.0
     */
    @PutMapping("/{id}/version/{version}/createAsset")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.CREATE_CONCEPT_SET) and @authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    public ConceptSetDTO copyAssetFromVersion(@PathVariable("id") final int id, @PathVariable("version") final int version) {
        checkVersion(id, version);
        var conceptSetVersion = versionService.getById(id, version);
        var conceptSet = conceptSetService.getConceptSet(id);

        var fullDTO = conversionService.convert(conceptSetVersion, ConceptSetVersionFullDTO.class);
        var conceptSetDTO = fullDTO.getEntityDTO();
        // Reset id so it won't be used during saving
        conceptSetDTO.setId(0);
        conceptSetDTO.setTags(null);
        conceptSetDTO.setName(
                NameUtils.getNameForCopy(conceptSetDTO.getName(), conceptSetService::getNamesLike, Optional.of(conceptSet)));
        var createdDTO = conceptSetService.createConceptSet(conceptSetDTO);
        conceptSetService.saveConceptSetItems(createdDTO.getId(), fullDTO.getItems().toArray(new ConceptSetItem[0]));

        return createdDTO;
    }

    /**
     * Get list of concept sets with their assigned tags
     *
     * @param requestDTO
     *         The tagNameListRequest
     * @return A list of concept sets with their assigned tags
     */
    @PostMapping("/byTags")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_CONCEPT_SET)")
    @Consumes(MediaType.APPLICATION_JSON)
    public List<ConceptSetDTO> listByTags(@RequestBody TagNameListRequestDTO requestDTO) {
        return conceptSetService.listByTags(requestDTO).stream()
                .filter(!webApiProperties.security().defaultGlobalReadPermissions()
                        ? authorizationManager::currentUserCanRead
                        : entity -> true)
                .map(entity -> {
                    return conversionService.convert(entity, ConceptSetDTO.class);
                })
                .collect(Collectors.toList());
    }

    private void checkUpdatePermission(int conceptSetId) {
        if (!authorizationManager.currentUserCanUpdate(SecuredEntityType.CONCEPT_SET, conceptSetId)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to update Concept Set with id = %d", conceptSetId));
        }
    }

    private void checkReadPermission(int conceptSetId) {
        if (webApiProperties.security().defaultGlobalReadPermissions()) {
            return;
        }
        if (!authorizationManager.currentUserCanRead(SecuredEntityType.CONCEPT_SET, conceptSetId)) {
            throw new AccessDeniedException(
                    String.format("You do not have permission to read Concept Set with id = %d", conceptSetId));
        }
    }

    private void checkVersion(int conceptSetId, int version) {
        var conceptSetVersion = versionService.getById(conceptSetId, version);
        if (conceptSetVersion == null) {
            throw new NotFoundException(String.format("There is no concept set version with id = %d.", version));
        }
        checkReadPermission(conceptSetId);
    }
}
