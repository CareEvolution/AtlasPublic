package org.ohdsi.webapi.cohortcharacterization;

import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterization;
import org.ohdsi.webapi.cohortcharacterization.domain.CcGenerationEntity;
import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.cohortcharacterization.dto.*;
import org.ohdsi.webapi.cohortdefinition.event.CohortDefinitionChangedEvent;
import org.ohdsi.webapi.conceptset.ConceptSetExport;
import org.ohdsi.webapi.feanalysis.event.FeAnalysisChangedEvent;
import org.ohdsi.webapi.job.JobExecutionResource;
import org.ohdsi.webapi.tag.dto.TagNameListRequestDTO;
import org.ohdsi.webapi.versioning.domain.CharacterizationVersion;
import org.ohdsi.webapi.versioning.dto.VersionDTO;
import org.ohdsi.webapi.versioning.dto.VersionUpdateDTO;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.OutputStream;
import java.util.List;

public interface CcService {
    void assignTag(Long id, int tagId);
    void assignTag(List<Long> ids, int tagId);
    void unassignTag(Long id, int tagId);
    void unassignTag(List<Long> ids, int tagId);
    
    CohortCharacterizationEntity createCc(CohortCharacterizationEntity entity);

    CohortCharacterizationEntity updateCc(CohortCharacterizationEntity entity);

    int getCountCcWithSameName(Long id, String name);

    void deleteCc(Long ccId);

    CohortCharacterizationEntity importCc(CohortCharacterizationEntity entity);

    String getNameForCopy(String dtoName);

    String getNameWithSuffix(String dtoName);

    String serializeCc(Long id);

    String serializeCc(CohortCharacterizationEntity cohortCharacterizationEntity);

    CohortCharacterizationEntity findById(Long id);

    CohortCharacterizationEntity findByIdWithLinkedEntities(Long id);

    CcExportDTO findDesignByGenerationId(final Long id);

    Page<CohortCharacterizationEntity> getPageWithLinkedEntities(Pageable pageable);

    Page<CohortCharacterizationEntity> getPage(Pageable pageable);

    JobExecutionResource generateCc(Long id, final String sourceKey);

    List<CcGenerationEntity> findGenerationsByCcId(Long id);

    CcGenerationEntity findGenerationById(final Long id);

    List<CcGenerationEntity> findGenerationsByCcIdAndSource(Long id, String sourceKey);

    GenerationResults findResult(Long generationId, ExecutionResultRequest params);

    List<CcResult> findResultAsList(Long generationId, float thresholdLevel);

    List<CcPrevalenceStat> getPrevalenceStatsByGenerationId(final Long id, Long analysisId, final Long cohortId,
            final Long covariateId);

    void hydrateAnalysis(Long analysisId, String packageName, OutputStream out);

    void deleteCcGeneration(Long generationId);

    void cancelGeneration(Long id, String sourceKey);

    Long getCCResultsTotalCount(Long id);

    List<ConceptSetExport> exportConceptSets(CohortCharacterization cohortCharacterization);

    GenerationResults exportExecutionResult(Long generationId, ExportExecutionResultRequest params);

    GenerationResults findData(final Long generationId, ExecutionResultRequest params);

    @EventListener
    void onCohortDefinitionChanged(CohortDefinitionChangedEvent event);

    @EventListener
    void onFeAnalysisChanged(FeAnalysisChangedEvent event);

    List<VersionDTO> getVersions(long id);

    CcVersionFullDTO getVersion(long id, int version);

    VersionDTO updateVersion(long id, int version, VersionUpdateDTO updateDTO);

    void deleteVersion(long id, int version);

    CohortCharacterizationEntity copyAssetFromVersion(long id, int version);

    CharacterizationVersion saveVersion(long id);

    List<CohortCharacterizationEntity> listByTags(TagNameListRequestDTO requestDTO);

    CohortCharacterizationEntity copyCohortCharacterization(Long id);
}
