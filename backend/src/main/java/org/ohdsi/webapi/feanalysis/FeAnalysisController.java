package org.ohdsi.webapi.feanalysis;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisDomain;
import org.ohdsi.webapi.Pagination;
import org.ohdsi.webapi.conceptset.ConceptSetExport;
import org.ohdsi.webapi.feanalysis.domain.*;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisAggregateDTO;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisDTO;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisShortDTO;
import org.ohdsi.webapi.feanalysis.dto.OptionDTO;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.model.SecuredEntityType;
import org.ohdsi.webapi.util.ExportUtil;
import org.ohdsi.webapi.util.HttpUtils;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for working with feature analyses. Feature analyses are used to generate descriptive statistics 
 * about a cohort of interest.
 */
@RestController
@RequestMapping("/feature-analysis")
public class FeAnalysisController {
    private final AuthorizationManager authorizationManager;
    private final FeAnalysisService service;
    private final ConversionService conversionService;
    private final WebApiProperties webApiProperties;

    FeAnalysisController(
            final AuthorizationManager authorizationManager,
            final FeAnalysisService service,
            final ConversionService conversionService,
            final WebApiProperties webApiProperties) {
        this.authorizationManager = authorizationManager;
        this.service = service;
        this.conversionService = conversionService;
        this.webApiProperties = webApiProperties;
    }

    /**
     * Get a pagable list of all feature analyses available in WebAPI
     *
     * @param pageable
     * @return
     */
    @GetMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_FEATURE_ANALYSIS)")
    public Page<FeAnalysisShortDTO> list(@Pagination Pageable pageable) {
        var analyses = service.getPage(pageable)
            .stream()
            .filter(entity -> {
                if (entity.isPreset()) {
                    return true;
                }
                if (webApiProperties.security().defaultGlobalReadPermissions()) {
                    return true;
                }
                return authorizationManager.currentUserCanRead(entity);
            })
            .map(entity -> convertFeAnaysisToShortDto(entity))
            .collect(Collectors.toList());

        int startIndex = pageable.getPageNumber() * pageable.getPageSize();
        int endIndex = Math.min(startIndex + pageable.getPageSize(), analyses.size());

        return new PageImpl<>(analyses.subList(startIndex, endIndex), pageable, analyses.size());
    }

    /**
     * Does a feature analysis name already exist?
     *
     * @param id
     *         The id for a new feature analysis that does not already exist
     * @param name
     *         The desired name for the new feature analysis
     * @return 1 if the name conflicts with an existing feature analysis name and 0 otherwise
     */
    @GetMapping("/{id}/exists")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_FEATURE_ANALYSIS)")
    public int getCountFeWithSameName(
            @PathVariable("id") final int id,
            @RequestParam String name) {
        return service.getCountFeWithSameName(id, name);
    }

    /**
     * Feature analysis domains
     *
     * @return Feature analysis domains such as DRUG, DRUG_ERA, MEASUREMENT, etc.
     */
    @GetMapping("/domains")
    // No preauthorization needed, domains are available to everyone
    public List<OptionDTO> listDomains() {
        var options = new ArrayList<OptionDTO>();
        for (var enumEntry : StandardFeatureAnalysisDomain.values()) {
            options.add(new OptionDTO(enumEntry.name(), enumEntry.getName()));
        }
        return options;
    }

    /**
     * Create a new feature analysis
     *
     * @param dto
     *         Feature analysis specification
     * @return
     */
    @PostMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.CREATE_FEATURE_ANALYSIS)")
    public FeAnalysisDTO createAnalysis(@RequestBody final FeAnalysisDTO dto) {
        final var createdEntity = service
                .createAnalysis(conversionService.convert(dto, FeAnalysisEntity.class));
        return convertFeAnalysisToDto(createdEntity);
    }

    /**
     * Update an existing feature analysis
     *
     * @param feAnalysisId
     *         ID of Feature analysis to update
     * @param dto
     *         Feature analysis specification
     * @return
     */
    @PutMapping("/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_FEATURE_ANALYSIS)")
    public FeAnalysisDTO updateAnalysis(
            @PathVariable("id") final Integer feAnalysisId,
            @RequestBody final FeAnalysisDTO dto) {
        checkUpdatePermission(feAnalysisId);
        final var updatedEntity = service.updateAnalysis(feAnalysisId,
                conversionService.convert(dto, FeAnalysisEntity.class));
        return convertFeAnalysisToDto(updatedEntity);
    }

    /**
     * Delete a feature analysis
     *
     * @param feAnalysisId
     *         ID of feature analysis to delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.DELETE_FEATURE_ANALYSIS)")
    public void deleteAnalysis(@PathVariable("id") final Integer feAnalysisId) {
        final var entity = service.findById(feAnalysisId).orElseThrow(NotFoundException::new);
        if (entity.isPreset()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to delete preset feature analyses");
        }
        if (!authorizationManager.currentUserCanDelete(SecuredEntityType.FE_ANALYSIS, feAnalysisId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to delete this feature analysis");
        }
        service.deleteAnalysis(entity);
    }

    /**
     * Get data about a specific feature analysis
     *
     * @param feAnalysisId
     *         ID of feature analysis to retrieve
     * @return ID, type, name domain, description, etc of feature analysis
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_FEATURE_ANALYSIS)")
    public FeAnalysisDTO getFeAnalysis(@PathVariable("id") final Integer feAnalysisId) {
        checkReadPermission(feAnalysisId);
        final var feAnalysis = service.findById(feAnalysisId)
                .orElseThrow(NotFoundException::new);
        return convertFeAnalysisToDto(feAnalysis);
    }

    @GetMapping("/{id}/export/conceptset")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_FEATURE_ANALYSIS)")
    public ResponseEntity<ByteArrayResource> exportConceptSets(@PathVariable("id") final Integer feAnalysisId) {
        checkReadPermission(feAnalysisId);
        final var feAnalysis = service.findById(feAnalysisId).orElseThrow(NotFoundException::new);
        if (feAnalysis instanceof FeAnalysisWithCriteriaEntity) {
            List<ConceptSetExport> exportList = service.exportConceptSets((FeAnalysisWithCriteriaEntity<?>) feAnalysis);
            ByteArrayOutputStream stream = ExportUtil.writeConceptSetExportToCSVAndZip(exportList);
            var filename = String.format("featureAnalysis_%d_export.zip", feAnalysisId);
            return HttpUtils.respondBinary(stream, filename);
        } else {
            throw new BadRequestException();
        }
    }

    /**
     * Create a copy of a feature analysis
     *
     * @param feAnalysisId
     *         ID of feature analysis to copy
     * @return The design specification of the new copy
     */
    @GetMapping("/{id}/copy")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.CREATE_FEATURE_ANALYSIS)")
    @Transactional
    public FeAnalysisDTO copy(@PathVariable("id") final Integer feAnalysisId) {
        checkReadPermission(feAnalysisId);
        final var feAnalysis = service.findById(feAnalysisId)
                .orElseThrow(NotFoundException::new);
        final var feAnalysisForCopy = service.getNewEntityForCopy(feAnalysis);

        return switch (feAnalysis.getType()) {
            case CRITERIA_SET -> {
                var savedCriteriaSet = service.createCriteriaAnalysis((FeAnalysisWithCriteriaEntity<?>) feAnalysisForCopy);
                yield convertFeAnalysisToDto(savedCriteriaSet);
            }
            case CUSTOM_FE -> {
                var savedCustomAnalysis = service.createAnalysis(feAnalysisForCopy);
                yield convertFeAnalysisToDto(savedCustomAnalysis);
            }
            default -> throw new IllegalArgumentException(
                    "Analysis with type: " + feAnalysis.getType() + " cannot be copied");
        };
    }

    /**
     * Get aggregation functions used in feature analyses
     *
     * @return
     */
    @GetMapping("/aggregates")
    // No preauthorization needed, aggregates are available to everyone
    public List<FeAnalysisAggregateDTO> listAggregates() {
        var result = service.findAggregates().stream()
                .map(this::convertFeAnalysisAggregateToDto)
                .collect(Collectors.toList());
        return result;
    }

    private FeAnalysisShortDTO convertFeAnaysisToShortDto(final FeAnalysisEntity<?> entity) {
        var dto = conversionService.convert(entity, FeAnalysisShortDTO.class);
        authorizationManager.fillPermissions(entity, dto);
        return dto;
    }

    private FeAnalysisDTO convertFeAnalysisToDto(final FeAnalysisEntity<?> entity) {
        var dto = conversionService.convert(entity, FeAnalysisDTO.class);
        authorizationManager.fillPermissions(entity, dto);
        return dto;
    }

    private FeAnalysisAggregateDTO convertFeAnalysisAggregateToDto(final FeAnalysisAggregateEntity entity) {
        return conversionService.convert(entity, FeAnalysisAggregateDTO.class);
    }

    private void checkReadPermission(int feAnalysisId) {
        var feAnalysis = service.findById(feAnalysisId).orElseThrow(NotFoundException::new);
        if (feAnalysis.isPreset()) {
            return;
        }
        if (webApiProperties.security().defaultGlobalReadPermissions()) {
            return;
        }
        if (!authorizationManager.currentUserCanRead(SecuredEntityType.FE_ANALYSIS, feAnalysisId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to read this feature analysis");
        }
    }

    private void checkUpdatePermission(int feAnalysisId) {
        var feAnalysis = service.findById(feAnalysisId).orElseThrow(NotFoundException::new);
        if (feAnalysis.isPreset()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to update preset feature analyses");
        }
        if (!authorizationManager.currentUserCanUpdate(SecuredEntityType.FE_ANALYSIS, feAnalysisId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have permission to update this feature analysis");
        }
    }
}
