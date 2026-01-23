package org.ohdsi.webapi.cohortcharacterization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.analysis.Utils;
import org.ohdsi.analysis.WithId;
import org.ohdsi.analysis.cohortcharacterization.design.CohortCharacterization;
import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType;
import org.ohdsi.featureExtraction.FeatureExtraction;
import org.ohdsi.hydra.Hydra;
import org.ohdsi.sql.SqlSplit;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.Constants;
import org.ohdsi.webapi.cohortcharacterization.converter.CharacterizationVersionToCharacterizationVersionFullDTOConverter;
import org.ohdsi.webapi.cohortcharacterization.converter.SerializedCcToCcConverter;
import org.ohdsi.webapi.cohortcharacterization.domain.*;
import org.ohdsi.webapi.cohortcharacterization.dto.*;
import org.ohdsi.webapi.cohortcharacterization.report.AnalysisItem;
import org.ohdsi.webapi.cohortcharacterization.report.AnalysisResultItem;
import org.ohdsi.webapi.cohortcharacterization.report.Report;
import org.ohdsi.webapi.cohortcharacterization.repository.*;
import org.ohdsi.webapi.cohortcharacterization.specification.CohortCharacterizationImpl;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionEntity;
import org.ohdsi.webapi.cohortdefinition.event.CohortDefinitionChangedEvent;
import org.ohdsi.webapi.common.DesignImportService;
import org.ohdsi.webapi.common.generation.GenerationUtils;
import org.ohdsi.webapi.conceptset.ConceptSetExport;
import org.ohdsi.webapi.feanalysis.FeAnalysisService;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisCriteriaEntity;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisEntity;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisWithCriteriaEntity;
import org.ohdsi.webapi.feanalysis.domain.FeAnalysisWithStringEntity;
import org.ohdsi.webapi.feanalysis.event.FeAnalysisChangedEvent;
import org.ohdsi.webapi.job.GeneratesNotification;
import org.ohdsi.webapi.job.JobExecutionResource;
import org.ohdsi.webapi.job.JobService;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.converter.UserDtoToUserEntity;
import org.ohdsi.webapi.authorization.model.SecuredEntityType;
import org.ohdsi.webapi.authorization.repository.UserRepository;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceInfo;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.sqlrender.SourceAwareSqlRender;
import org.ohdsi.webapi.tag.TagService;
import org.ohdsi.webapi.tag.dto.TagNameListRequestDTO;
import org.ohdsi.webapi.transactiontemplates.TransactionTemplates;
import org.ohdsi.webapi.util.*;
import org.ohdsi.webapi.versioning.VersionBaseToVersionDTOConverter;
import org.ohdsi.webapi.versioning.VersionToVersionDTOConverter;
import org.ohdsi.webapi.versioning.domain.CharacterizationVersion;
import org.ohdsi.webapi.versioning.dto.VersionDTO;
import org.ohdsi.webapi.versioning.dto.VersionUpdateDTO;
import org.ohdsi.webapi.versioning.service.CohortCharacterizationVersionService;
import org.ohdsi.webapi.vocabulary.VocabularyService;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.ohdsi.analysis.cohortcharacterization.design.CcResultType.DISTRIBUTION;
import static org.ohdsi.analysis.cohortcharacterization.design.CcResultType.PREVALENCE;
import static org.ohdsi.webapi.Constants.GENERATE_COHORT_CHARACTERIZATION;
import static org.ohdsi.webapi.Constants.Params.*;

// TODO this class is monstrously large, needs to be refactored.
@Service
public class CcServiceImpl implements CcService, GeneratesNotification {
    final Logger log = LoggerFactory.getLogger(CcServiceImpl.class);

    private static final String GENERATION_NOT_FOUND_ERROR = "generation cannot be found by id %d";
    private static final String[] PARAMETERS_RESULTS = { "cohort_characterization_generation_id", "threshold_level",
            "vocabulary_schema" };
    private static final String[] PARAMETERS_RESULTS_FILTERED = { "cohort_characterization_generation_id",
            "threshold_level",
            "analysis_ids", "cohort_ids", "vocabulary_schema" };
    private static final String[] PARAMETERS_COUNT = { "cohort_characterization_generation_id", "vocabulary_schema" };
    private static final String[] PREVALENCE_STATS_PARAMS = { "vocab_database_schema", "cdm_results_schema",
            "cc_generation_id", "analysis_id", "cohort_id", "covariate_id" };


    private final static List<String> INCOMPLETE_STATUSES = ImmutableList.of(BatchStatus.STARTED, BatchStatus.STARTING,
            BatchStatus.STOPPING, BatchStatus.UNKNOWN)
            .stream().map(BatchStatus::name).collect(Collectors.toList());

    private final Map<String, FeatureExtraction.PrespecAnalysis> prespecAnalysisMap = FeatureExtraction
            .getNameToPrespecAnalysis();

    private final List<String[]> executionPrevalenceHeaderLines = new ArrayList<>() {
        {
            add(new String[] { "Analysis ID", "Analysis name", "Strata ID",
                    "Strata name", "Cohort ID", "Cohort name", "Covariate ID", "Covariate name", "Covariate short name",
                    "Count", "Percent" });
        }
    };

    private final List<String[]> executionDistributionHeaderLines = new ArrayList<>() {
        {
            add(new String[] { "Analysis ID", "Analysis name", "Strata ID",
                    "Strata name", "Cohort ID", "Cohort name", "Covariate ID", "Covariate name", "Covariate short name",
                    "Value field", "Missing Means Zero",
                    "Count", "Avg", "StdDev", "Min", "P10", "P25", "Median", "P75", "P90", "Max" });
        }
    };

    private final List<String[]> executionComparativeHeaderLines = new ArrayList<>() {
        {
            add(new String[] { "Analysis ID", "Analysis name", "Strata ID",
                    "Strata name", "Target cohort ID", "Target cohort name", "Comparator cohort ID",
                    "Comparator cohort name",
                    "Covariate ID", "Covariate name", "Covariate short name", "Target count", "Target percent",
                    "Comparator count", "Comparator percent", "Std. Diff Of Mean" });
        }
    };

    private final String queryResultsSql;
    private final String queryCountSql;
    private final String deleteResultsSql;
    private final String deleteExecutionSql;
    private final String queryPrevalenceStatsSql;

    private final CcRepository repository;
    private final CcParamRepository paramRepository;
    private final CcStrataRepository strataRepository;
    private final FeAnalysisService analysisService;
    private final CcGenerationEntityRepository ccGenerationRepository;
    private final DesignImportService designImportService;
    private final AnalysisGenerationInfoEntityRepository analysisGenerationInfoEntityRepository;
    private final SourceService sourceService;
    private final GenerationUtils generationUtils;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;
    private final SourceAwareSqlRender sourceAwareSqlRender;
    private final JobService jobService;
    private final ConversionService conversionService;
    private final VocabularyService vocabularyService;
    private final CohortCharacterizationVersionService versionService;
    private final CharacterizationVersionToCharacterizationVersionFullDTOConverter characterizationVersionToFullDTOConverter;
    private final AuthorizationManager authorizationManager;
    private final TagService tagService;
    private final TransactionTemplates transactionTemplates;
    private final UserRepository userRepository;
    private final JdbcTemplate webapiJdbcTemplate;
    private final StringResourceReader resourceReader;
    private final WebApiProperties webApiProperties;

    public CcServiceImpl(
            final AuthorizationManager authorizationManager,
            final CcRepository ccRepository,
            final CcParamRepository paramRepository,
            final CcStrataRepository strataRepository,
            final FeAnalysisService analysisService,
            final CcGenerationEntityRepository ccGenerationRepository,
            final ConversionService conversionService,
            final DesignImportService designImportService,
            final AnalysisGenerationInfoEntityRepository analysisGenerationInfoEntityRepository,
            final SourceService sourceService,
            final GenerationUtils generationUtils,
            final SourceAwareSqlRender sourceAwareSqlRender,
            final EntityManager entityManager,
            final JobService jobService,
            final ApplicationEventPublisher eventPublisher,
            final VocabularyService vocabularyService,
            final CohortCharacterizationVersionService versionService,
            final TagService tagService,
            final TransactionTemplates transactionTemplates,
            final UserRepository userRepository,
            final JdbcTemplate jdbcTemplate,
            final StringResourceReader resourceReader,
            final WebApiProperties webApiProperties,
            final CharacterizationVersionToCharacterizationVersionFullDTOConverter characterizationVersionToFullDTOConverter) {
        this.authorizationManager = authorizationManager;
        this.repository = ccRepository;
        this.paramRepository = paramRepository;
        this.strataRepository = strataRepository;
        this.analysisService = analysisService;
        this.ccGenerationRepository = ccGenerationRepository;
        this.designImportService = designImportService;
        this.analysisGenerationInfoEntityRepository = analysisGenerationInfoEntityRepository;
        this.sourceService = sourceService;
        this.generationUtils = generationUtils;
        this.sourceAwareSqlRender = sourceAwareSqlRender;
        this.entityManager = entityManager;
        this.jobService = jobService;
        this.eventPublisher = eventPublisher;
        this.vocabularyService = vocabularyService;
        this.tagService = tagService;
        this.transactionTemplates = transactionTemplates;
        this.userRepository = userRepository;
        this.webapiJdbcTemplate = jdbcTemplate;
        this.resourceReader = resourceReader;
        this.webApiProperties = webApiProperties;
        this.conversionService = conversionService;
        this.versionService = versionService;
        SerializedCcToCcConverter.setConversionService(conversionService);
        this.characterizationVersionToFullDTOConverter = characterizationVersionToFullDTOConverter;

        queryResultsSql = this.resourceReader.getText(
                "cohortcharacterizations/sql/queryResults.sql");
        queryCountSql = this.resourceReader.getText(
                "cohortcharacterizations/sql/queryCountWithoutThreshold.sql");
        deleteResultsSql = this.resourceReader.getText(
                "cohortcharacterizations/sql/deleteResults.sql");
        deleteExecutionSql = this.resourceReader.getText(
                "cohortcharacterizations/sql/deleteExecution.sql");
        queryPrevalenceStatsSql = this.resourceReader.getText(
                "cohortcharacterizations/sql/queryCovariateStatsVocab.sql");
    }

    @Override
    public CohortCharacterizationEntity createCc(final CohortCharacterizationEntity entity) {
        var currentUser = authorizationManager.getCurrentUser();
        var userEntity = UserDtoToUserEntity.convert(currentUser);
        entity.setCreatedBy(userEntity);
        entity.setCreatedDate(new Date());
        var savedEntity = saveCc(entity);
        authorizationManager.addOwnership(savedEntity);
        return savedEntity;
    }

    private CohortCharacterizationEntity saveCc(final CohortCharacterizationEntity entity) {
        var savedEntity = repository.saveAndFlush(entity);

        for (var strata : entity.getStratas()) {
            strata.setCohortCharacterization(savedEntity);
            strataRepository.save(strata);
        }

        for (var param : entity.getParameters()) {
            param.setCohortCharacterization(savedEntity);
            paramRepository.save(param);
        }

        entityManager.flush();
        entityManager.refresh(savedEntity);

        savedEntity = findByIdWithLinkedEntities(savedEntity.getId());

        var modifiedDate = savedEntity.getModifiedDate();
        savedEntity.setModifiedDate(null);
        final var serialized = this.serializeCc(savedEntity);
        savedEntity.setHashCode(serialized.hashCode());
        savedEntity.setModifiedDate(modifiedDate);

        return repository.save(savedEntity);
    }

    @EventListener
    @Transactional
    @Override
    public void onCohortDefinitionChanged(CohortDefinitionChangedEvent event) {
        var ccList = repository.findByCohortDefinition(event.getCohortDefinition());
        ccList.forEach(this::saveCc);
    }

    @EventListener
    @Transactional
    public void onFeAnalysisChanged(FeAnalysisChangedEvent event) {
        var ccList = repository.findByFeatureAnalysis(event.getFeAnalysis());
        ccList.forEach(this::saveCc);
    }

    public void assignTag(Long id, int tagId) {
        var entity = findById(id);
        tagService.assignTag(entity, tagId);
    }

    public void assignTag(List<Long> ids, int tagId) {
        ids.forEach(id -> {
            try {
                assignTag(id, tagId);
            } catch (final AccessDeniedException e) {
                log.warn("Tag {} cannot be assigned to entity {} - forbidden", tagId, id);
            }
        });
    }

    public void unassignTag(Long id, int tagId) {
        var entity = findById(id);
        tagService.unassignTag(entity, tagId);
    }

    public void unassignTag(List<Long> ids, int tagId) {
        ids.forEach(id -> {
            try {
                unassignTag(id, tagId);
            } catch (final AccessDeniedException e) {
                log.warn("Tag {} cannot be unassigned from entity {} - forbidden", tagId, id);
            }
        });
    }

    @Override
    public int getCountCcWithSameName(Long id, String name) {
        return repository.getCountCcWithSameName(id, name);
    }

    @Override
    @Transactional
    public void deleteCc(Long ccId) {
        repository.deleteById(ccId);
        authorizationManager.removeOwnership(SecuredEntityType.COHORT_CHARACTERIZATION, ccId);
    }

    @Override
    @Transactional
    public CohortCharacterizationEntity updateCc(final CohortCharacterizationEntity entity) {
        final var foundEntity = repository.findById(entity.getId())
                .orElseThrow(() -> new NotFoundException("CC entity isn't found"));

        updateLinkedFields(entity, foundEntity);

        if (StringUtils.isNotEmpty(entity.getName())) {
            foundEntity.setName(entity.getName());
        }
        foundEntity.setDescription(entity.getDescription());
        foundEntity.setStratifiedBy(entity.getStratifiedBy());
        if (Objects.nonNull(entity.getStrataOnly())) {
            foundEntity.setStrataOnly(entity.getStrataOnly());
        }

        foundEntity.setModifiedDate(new Date());
        var currentUser = authorizationManager.getCurrentUser();
        var userEntity = UserDtoToUserEntity.convert(currentUser);
        foundEntity.setModifiedBy(userEntity);

        return saveCc(foundEntity);
    }

    private void updateLinkedFields(final CohortCharacterizationEntity entity,
            final CohortCharacterizationEntity foundEntity) {
        updateConceptSet(entity, foundEntity);
        updateParams(entity, foundEntity);
        updateAnalyses(entity, foundEntity);
        updateCohorts(entity, foundEntity);
        updateStratas(entity, foundEntity);
    }

    private void updateConceptSet(CohortCharacterizationEntity entity, CohortCharacterizationEntity foundEntity) {
        if (Objects.nonNull(foundEntity.getConceptSetEntity()) && Objects.nonNull(entity.getConceptSetEntity())) {
            foundEntity.getConceptSetEntity().setRawExpression(entity.getConceptSetEntity().getRawExpression());
        } else if (Objects.nonNull(entity.getConceptSetEntity())) {
            var cse = new CcStrataConceptSetEntity();
            cse.setCohortCharacterization(foundEntity);
            cse.setRawExpression(entity.getConceptSetEntity().getRawExpression());
            foundEntity.setConceptSetEntity(cse);
        } else {
            foundEntity.setConceptSetEntity(null);
        }
    }

    private void updateStratas(CohortCharacterizationEntity entity, CohortCharacterizationEntity foundEntity) {
        final var stratasToDelete = getLinksToDelete(foundEntity,
                existingLink -> entity.getStratas().stream()
                        .noneMatch(newLink -> Objects.equals(newLink.getId(), existingLink.getId())),
                CohortCharacterizationEntity::getStratas);
        foundEntity.getStratas().removeAll(stratasToDelete);
        strataRepository.deleteAll(stratasToDelete);
        var strataEntityMap = foundEntity.getStratas().stream()
                .collect(Collectors.toMap(CcStrataEntity::getId, s -> s));

        var updatedStratas = entity.getStratas().stream().map(updated -> {
            updated.setCohortCharacterization(foundEntity);
            if (Objects.nonNull(updated.getId())) {
                var strata = strataEntityMap.get(updated.getId());
                // strata will be null in case of importing new characterization
                if (strata == null) {
                    return updated;
                }
                if (StringUtils.isNotBlank(updated.getName())) {
                    strata.setName(updated.getName());
                }
                strata.setExpressionString(updated.getExpressionString());
                return strata;
            } else {
                return updated;
            }
        }).collect(Collectors.toList());
        entity.setStratas(new HashSet<>(strataRepository.saveAll(updatedStratas)));
    }

    private void updateCohorts(final CohortCharacterizationEntity entity,
            final CohortCharacterizationEntity foundEntity) {
        foundEntity.getCohortDefinitions().clear();
        foundEntity.getCohortDefinitions().addAll(entity.getCohortDefinitions());
    }

    private void updateAnalyses(final CohortCharacterizationEntity entity,
            final CohortCharacterizationEntity foundEntity) {
        foundEntity.getFeatureAnalyses().clear();
        foundEntity.getFeatureAnalyses().addAll(entity.getFeatureAnalyses());
    }

    // TODO fix all uses of raw parameterized classes
    private <T extends WithId> List<T> getLinksToDelete(final CohortCharacterizationEntity foundEntity,
            Predicate<? super T> filterPredicate,
            Function<CohortCharacterizationEntity, Set<T>> getter) {
        return getter.apply(foundEntity)
                .stream()
                .filter(filterPredicate)
                .collect(Collectors.toList());
    }

    private void updateParams(final CohortCharacterizationEntity entity,
            final CohortCharacterizationEntity foundEntity) {
        updateOrCreateParams(entity, foundEntity);
        deleteParams(entity, foundEntity);
    }

    private void deleteParams(final CohortCharacterizationEntity entity,
            final CohortCharacterizationEntity foundEntity) {
        final var nameToParamFromInputMap = buildParamNameToParamMap(entity);
        var paramsForDelete = getLinksToDelete(foundEntity,
                parameter -> !nameToParamFromInputMap.containsKey(parameter.getName()),
                CohortCharacterizationEntity::getParameters);
        foundEntity.getParameters().removeAll(paramsForDelete);
        paramRepository.deleteAll(paramsForDelete);
    }

    private void updateOrCreateParams(final CohortCharacterizationEntity entity,
            final CohortCharacterizationEntity foundEntity) {
        final var nameToParamFromDbMap = buildParamNameToParamMap(foundEntity);
        final var paramsForCreateOrUpdate = new ArrayList<CcParamEntity>();
        for (final var parameter : entity.getParameters()) {
            final var entityFromMap = nameToParamFromDbMap.get(parameter.getName());
            parameter.setCohortCharacterization(foundEntity);
            if (entityFromMap == null) {
                paramsForCreateOrUpdate.add(parameter);
            } else if (!StringUtils.equals(entityFromMap.getValue(), parameter.getValue())) {
                entityFromMap.setValue(parameter.getValue());
                paramsForCreateOrUpdate.add(entityFromMap);
            }
        }
        paramRepository.saveAll(paramsForCreateOrUpdate);
    }

    @Override
    @Transactional
    public CohortCharacterizationEntity importCc(final CohortCharacterizationEntity entity) {
        cleanIds(entity);

        final var newCohortCharacterization = new CohortCharacterizationEntity();
        newCohortCharacterization.setName(entity.getName());
        final var persistedCohortCharacterization = this.createCc(newCohortCharacterization);

        updateParams(entity, persistedCohortCharacterization);
        updateStratas(entity, persistedCohortCharacterization);
        updateConceptSet(entity, persistedCohortCharacterization);

        importCohorts(entity, persistedCohortCharacterization);
        var savedAnalysesIds = importAnalyses(entity, persistedCohortCharacterization);

        final var savedEntity = saveCc(persistedCohortCharacterization);
        eventPublisher.publishEvent(new CcImportEvent(savedAnalysesIds));

        return savedEntity;
    }

    @Override
    public CohortCharacterizationEntity copyCohortCharacterization(Long id) {
        var existingCC = findByIdWithLinkedEntities(id);
        // I'm going to convert to DTO and back to entity to create a deep copy
        var ccDto = conversionService.convert(existingCC, CohortCharacterizationDTO.class);
        var cc = conversionService.convert(ccDto, CohortCharacterizationEntity.class);
        cc.setName(getNameForCopy(cc.getName()));
        cc.setId(null);
        cc.setTags(null);
        cc.getStratas().forEach(s -> s.setId(null));
        cc.getParameters().forEach(p -> p.setId(null));
        final var createdEntity = createCc(cc);
        return createdEntity;
    }

    @Override
    public String getNameForCopy(String dtoName) {
        return NameUtils.getNameForCopy(dtoName, this::getNamesLike, repository.findByName(dtoName));
    }

    @Override
    public String getNameWithSuffix(String dtoName) {
        return NameUtils.getNameWithSuffix(dtoName, this::getNamesLike);
    }

    @Override
    public String serializeCc(final Long id) {
        return Utils.serialize(exportCc(id), true);
    }

    @Override
    public String serializeCc(final CohortCharacterizationEntity cohortCharacterizationEntity) {
        return new SerializedCcToCcConverter().convertToDatabaseColumn(cohortCharacterizationEntity);
    }

    private CohortCharacterizationImpl exportCc(final Long id) {
        final CohortCharacterizationEntity cohortCharacterizationEntity = repository.findById(id)
                .orElseThrow(
                        () -> new IllegalArgumentException("Cohort characterization cannot be found by id: " + id));
        var cc = conversionService.convert(cohortCharacterizationEntity,
                CohortCharacterizationImpl.class);
        ExportUtil.clearCreateAndUpdateInfo(cc);
        cc.getFeatureAnalyses().forEach(ExportUtil::clearCreateAndUpdateInfo);
        cc.getCohorts().forEach(ExportUtil::clearCreateAndUpdateInfo);
        cc.setOrganizationName(webApiProperties.characterization().getOrganizationName());
        return cc;
    }

    @Override
    public CohortCharacterizationEntity findById(final Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cohort characterization with id: " + id + " cannot be found"));
    }

    @Override
    public CohortCharacterizationEntity findByIdWithLinkedEntities(final Long id) {
        return repository.findByIdEagerLoad(id)
                .orElseThrow(() -> new IllegalArgumentException(
                "Cohort characterization with id: " + id + " cannot be found"));
    }

    @Override
    public CcExportDTO findDesignByGenerationId(final Long id) {
        final var entity = analysisGenerationInfoEntityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Analysis with id: " + id + " cannot be found"));
        var ccExportDto = Utils.deserialize(entity.getDesign(),
                new TypeReference<CcExportDTO>() {
                });
        return ccExportDto;
    }

    @Override
    public Page<CohortCharacterizationEntity> getPageWithLinkedEntities(final Pageable pageable) {
        var ccList = repository.findAllEagerLoad(pageable)
                .stream().filter(!webApiProperties.security().defaultGlobalReadPermissions()
                        ? authorizationManager::currentUserCanRead
                        : entity -> true)
                .collect(Collectors.toList());
        return getPageFromResults(pageable, ccList);

    }

    @Override
    public Page<CohortCharacterizationEntity> getPage(final Pageable pageable) {
        var ccList = repository.findAll()
                .stream().filter(!webApiProperties.security().defaultGlobalReadPermissions()
                        ? authorizationManager::currentUserCanRead
                        : entity -> true)
                .collect(Collectors.toList());
        return getPageFromResults(pageable, ccList);
    }

    private Page<CohortCharacterizationEntity> getPageFromResults(Pageable pageable,
            List<CohortCharacterizationEntity> results) {
        // Calculate the start and end indices for the current page
        int startIndex = pageable.getPageNumber() * pageable.getPageSize();
        int endIndex = Math.min(startIndex + pageable.getPageSize(), results.size());

        return new PageImpl<>(results.subList(startIndex, endIndex), pageable, results.size());
    }

    @Override
    public void hydrateAnalysis(Long analysisId, String packageName, OutputStream out) {
        if (packageName == null || !isAlphaNumericOrDot(packageName)) {
            throw new IllegalArgumentException("The package name must be alphanumeric only.");
        }
        var analysis = exportCc(analysisId);
        analysis.setPackageName(packageName);
        var studySpecs = Utils.serialize(analysis, true);
        var hydra = new Hydra(studySpecs);
        File skeletonFile = null;
        try {
            skeletonFile = TempFileUtils.copyResourceToTempFile(
                    "cohortcharacterizations/hydra/CohortCharacterization_v0.0.1.zip",
                    "cc-",
                    ".zip");
            hydra.setExternalSkeletonFileName(skeletonFile.getAbsolutePath());
            hydra.hydrate(out);
        } catch (IOException e) {
            log.error("Failed to hydrate cohort characterization", e);
            throw new InternalServerErrorException(e);
        } finally {
            FileUtils.deleteQuietly(skeletonFile);
        }
    }

    private static boolean isAlphaNumericOrDot(String str) {

        Pattern p = Pattern.compile("^[a-zA-Z0-9.]+$");
        Matcher m = p.matcher(str);
        return m.find();
    }

    @Override
    public JobExecutionResource generateCc(final Long id, final String sourceKey) {
        var ccService = this;
        var source = sourceService.findBySourceKey(sourceKey);
        var builder = new JobParametersBuilder();

        builder.addString(JOB_NAME,
                String.format("Generating cohort characterization %d : %s (%s)", id, source.getSourceName(),
                        source.getSourceKey()));
        builder.addString(COHORT_CHARACTERIZATION_ID, String.valueOf(id));
        builder.addString(SOURCE_ID, String.valueOf(source.getSourceId()));
        builder.addString(JOB_AUTHOR, authorizationManager.getCurrentUser().login());

        CancelableJdbcTemplate jdbcTemplate = sourceService.getSourceJdbcTemplate(source);

        var generateCohortJob = generationUtils.buildJobForCohortBasedAnalysisTasklet(
                GENERATE_COHORT_CHARACTERIZATION,
                source,
                builder,
                jdbcTemplate,
                chunkContext -> {
                    Long ccId = Long.valueOf(
                            chunkContext.getStepContext().getJobParameters().get(COHORT_CHARACTERIZATION_ID)
                                    .toString());
                    return ccService.findById(ccId).getCohortDefinitions();
                },
                new GenerateCohortCharacterizationTasklet(
                        jdbcTemplate,
                        transactionTemplates.getTransactionTemplate(),
                        ccService,
                        analysisGenerationInfoEntityRepository,
                        sourceService,
                        userRepository));

        final JobParameters jobParameters = builder.toJobParameters();
        return jobService.runJob(generateCohortJob, jobParameters);
    }

    @Override
    public List<CcGenerationEntity> findGenerationsByCcId(final Long id) {
        return ccGenerationRepository.findByCohortCharacterizationIdOrderByIdDesc(id);
    }

    @Override
    public CcGenerationEntity findGenerationById(final Long id) {
        var optionalCC = ccGenerationRepository.findByIdEagerLoad(id);
        if (optionalCC.isEmpty()) {
            throw new NotFoundException(String.format(GENERATION_NOT_FOUND_ERROR, id));
        }
        return optionalCC.get();
    }

    @Override
    public List<CcGenerationEntity> findGenerationsByCcIdAndSource(final Long id, final String sourceKey) {
        return ccGenerationRepository.findByCohortCharacterizationIdAndSourceSourceKeyOrderByIdDesc(id, sourceKey);
    }

    public List<CcGenerationEntity> findAllIncompleteGenerations() {
        return ccGenerationRepository.findByStatusIn(INCOMPLETE_STATUSES);
    }

    protected List<CcResult> findResults(final Long generationId, ExecutionResultRequest params) {
        final var generationEntity = ccGenerationRepository.findById(generationId)
                .orElseThrow(
                        () -> new IllegalArgumentException(String.format(GENERATION_NOT_FOUND_ERROR, generationId)));
        final var source = generationEntity.getSource();
        var analysis = params.getAnalysisIds().stream().map(String::valueOf).collect(Collectors.joining(","));
        var cohorts = params.getCohortIds().stream().map(String::valueOf).collect(Collectors.joining(","));
        var generationResults = sourceAwareSqlRender.renderSql(source.getSourceId(), queryResultsSql,
                PARAMETERS_RESULTS_FILTERED,
                new String[] { String.valueOf(generationId), String.valueOf(params.getThresholdValuePct()),
                        analysis, cohorts, SourceUtils.getVocabularyQualifier(source) });
        final var tempSchema = SourceUtils.getTempQualifier(source);
        var translatedSql = SqlTranslate.translateSql(generationResults, source.getSourceDialect(),
                SessionUtils.sessionId(), tempSchema);
        return getGenerationResults(source, translatedSql);
    }

    @Override
    public Long getCCResultsTotalCount(final Long generationId) {
        final var generationEntity = ccGenerationRepository.findById(generationId)
                .orElseThrow(
                        () -> new IllegalArgumentException(String.format(GENERATION_NOT_FOUND_ERROR, generationId)));
        final var source = generationEntity.getSource();
        var countReq = sourceAwareSqlRender.renderSql(source.getSourceId(), queryCountSql, PARAMETERS_COUNT,
                new String[] { String.valueOf(generationId), SourceUtils.getVocabularyQualifier(source) });
        final var tempSchema = SourceUtils.getTempQualifier(source);
        var translatedSql = SqlTranslate.translateSql(countReq, source.getSourceDialect(), SessionUtils.sessionId(),
                tempSchema);
        return sourceService.getSourceJdbcTemplate(source).queryForObject(translatedSql, Long.class);
    }

    @Override
    public GenerationResults exportExecutionResult(final Long generationId, ExportExecutionResultRequest params) {
        var res = findResult(generationId, params);

        if (params.isFilterUsed()) {
            res.setReports(res.getReports().stream()
                    .filter(r -> params.isComparative() == null || params.isComparative() == r.isComparative)
                    .filter(r -> params.isSummary() == null || params.isSummary() == r.isSummary)
                    .collect(Collectors.toList()));
        }

        return res;
    }

    @Override
    public GenerationResults findData(final Long generationId, ExecutionResultRequest params) {
        if (params.getShowEmptyResults()) {
            params.setThresholdValuePct(
                    Constants.DEFAULT_THRESHOLD); //Don't cut threshold results when all results requested
        }
        var res = findResult(generationId, params);
        boolean hasComparativeReports = res.getReports().stream()
                .anyMatch(report -> report.isComparative);
        if (hasComparativeReports) {
            // If there are comparative reports - return only them as simple reports won't be shown on ui
            res.setReports(res.getReports().stream()
                    .filter(report -> report.isComparative)
                    .collect(Collectors.toList()));
        }
        res.setPrevalenceThreshold(params.getThresholdValuePct());
        return res;
    }

    @Override
    public List<CcResult> findResultAsList(final Long generationId, float thresholdLevel) {
        var params = new ExecutionResultRequest();
        var generationEntity = ccGenerationRepository.findById(generationId)
                .orElseThrow(
                        () -> new IllegalArgumentException(String.format(GENERATION_NOT_FOUND_ERROR, generationId)));
        var characterization = generationEntity.getCohortCharacterization();
        params.setThresholdValuePct(thresholdLevel);
        params.setCohortIds(characterization.getCohortDefinitions().stream()
                .map(CohortDefinitionEntity::getId).collect(Collectors.toList()));
        params.setAnalysisIds(characterization.getFeatureAnalyses().stream()
                .map(this::mapFeatureAnalysisId).collect(Collectors.toList()));
        params.setDomainIds(generationEntity.getCohortCharacterization().getFeatureAnalyses().stream()
                .map(fa -> fa.getDomain().toString()).distinct().collect(Collectors.toList()));
        return findResults(generationId, params);
    }

    @Override
    public GenerationResults findResult(final Long generationId, ExecutionResultRequest params) {
        var generationEntity = ccGenerationRepository.findById(generationId)
                .orElseThrow(
                        () -> new IllegalArgumentException(String.format(GENERATION_NOT_FOUND_ERROR, generationId)));

        var characterization = generationEntity.getCohortCharacterization();
        var cohortDefs = characterization.getCohorts();
        var featureAnalyses = characterization.getFeatureAnalyses();

        // if filter is not used then it must be initialized first
        if (!params.isFilterUsed()) {
            params.setCohortIds(characterization.getCohortDefinitions().stream()
                    .map(CohortDefinitionEntity::getId).collect(Collectors.toList()));
            params.setAnalysisIds(
                    featureAnalyses.stream().map(this::mapFeatureAnalysisId).collect(Collectors.toList()));
            params.setDomainIds(generationEntity.getCohortCharacterization().getFeatureAnalyses().stream()
                    .map(fa -> fa.getDomain().toString()).distinct().collect(Collectors.toList()));
        } else {
            var analysisIds = params.getAnalysisIds().stream().map(analysisId -> {
                var fe = featureAnalyses.stream()
                        .filter(fa -> Objects.equals(fa.getId(), analysisId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                                String.format("Feature with id=%s not found in analysis", analysisId)));
                return mapFeatureAnalysisId(fe);
            }).collect(Collectors.toList());
            params.setAnalysisIds(analysisIds);
        }
        // remove domains which cannot be used as corresponding analyses are not selected
        params.getDomainIds().removeIf(s -> featureAnalyses.stream()
                .noneMatch(fe -> fe.getDomain().toString().equals(s) && params.getAnalysisIds()
                        .contains(mapFeatureAnalysisId(fe))));
        // remove analyses which cannot be used as corresponding domains are not selected
        params.getAnalysisIds().removeIf(s -> featureAnalyses.stream()
                .noneMatch(fe -> mapFeatureAnalysisId(fe).equals(s) && params.getDomainIds()
                        .contains(fe.getDomain().toString())));

        var ccResults = findResults(generationId, params);

        // create initial structure and fill with results
        var analysisMap = new HashMap<Integer, AnalysisItem>();
        ccResults
                .stream()
                .peek(cc -> {
                    if (StandardFeatureAnalysisType.PRESET.toString().equals(cc.getFaType())) {
                        featureAnalyses.stream()
                                .filter(fa -> Objects.equals(fa.getDesign(), cc.getAnalysisName()))
                                .findFirst()
                                .ifPresent(v -> cc.setAnalysisId(v.getId()));
                    }
                })
                .forEach(ccResult -> {
                    if (ccResult instanceof CcPrevalenceStat) {
                        analysisMap.putIfAbsent(ccResult.getAnalysisId(), new AnalysisItem());
                        AnalysisItem analysisItem = analysisMap.get(ccResult.getAnalysisId());
                        analysisItem.setType(ccResult.getResultType());
                        analysisItem.setName(ccResult.getAnalysisName());
                        analysisItem.setFaType(ccResult.getFaType());
                        List<CcResult> results = analysisItem.getOrCreateCovariateItem(
                                ((CcPrevalenceStat) ccResult).getCovariateId(), ccResult.getStrataId());
                        results.add(ccResult);
                    }
                });

        cohortDefs = cohortDefs
                .stream()
                .filter(def -> params.getCohortIds().contains(def.getId()))
                .collect(Collectors.toSet());

        var reports = prepareReportData(analysisMap, cohortDefs, featureAnalyses);

        var res = new GenerationResults();
        res.setReports(reports);
        res.setCount(ccResults.size());
        return res;
    }

    // TODO fix raw use of parameterized class FeAnalysisEntity
    private Integer mapFeatureAnalysisId(FeAnalysisEntity feAnalysis) {
        if (feAnalysis.isPreset()) {
            return prespecAnalysisMap.values().stream()
                    .filter(p -> Objects.equals(p.analysisName, feAnalysis.getDesign()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Preset analysis with id=%s does not exist", feAnalysis.getId()))).analysisId;
        } else {
            return feAnalysis.getId();
        }
    }

    private String mapFeatureName(FeAnalysisEntity entity) {
        if (StandardFeatureAnalysisType.PRESET == entity.getType()) {
            return entity.getDesign().toString();
        }
        return entity.getName();
    }

    private List<Report> prepareReportData(Map<Integer, AnalysisItem> analysisMap, Set<CohortDefinitionEntity> cohortDefs,
            Set<FeAnalysisEntity> featureAnalyses) {
        // Create map to get cohort name by its id
        final var definitionMap = cohortDefs.stream()
                .collect(Collectors.toMap(CohortDefinitionEntity::getId, Function.identity()));
        // Create map to get feature analyses by its name
        final var feAnalysisMap = featureAnalyses.stream()
                .collect(Collectors.toMap(this::mapFeatureName, entity -> entity.getDomain().toString()));

        var reports = new ArrayList<Report>();
        try {
            // list to accumulate results from simple reports
            var simpleResultSummary = new ArrayList<AnalysisResultItem>();
            // list to accumulate results from comparative reports
            var comparativeResultSummary = new ArrayList<AnalysisResultItem>();
            // do not create summary reports when only one analyses is present
            boolean ignoreSummary = analysisMap.keySet().size() == 1;
            for (Integer analysisId : analysisMap.keySet()) {
                analysisMap.putIfAbsent(analysisId, new AnalysisItem());
                var analysisItem = analysisMap.get(analysisId);
                var resultItem = analysisItem.getSimpleItems(definitionMap, feAnalysisMap);
                var simpleReport = new Report(analysisItem.getName(), analysisId, resultItem);
                simpleReport.faType = analysisItem.getFaType();
                simpleReport.domainId = feAnalysisMap.get(analysisItem.getName());

                if (PREVALENCE.equals(analysisItem.getType())) {
                    simpleReport.header = executionPrevalenceHeaderLines;
                    simpleReport.resultType = PREVALENCE;
                    // Summary comparative reports are only available for prevalence type
                    simpleResultSummary.add(resultItem);
                } else if (DISTRIBUTION.equals(analysisItem.getType())) {
                    simpleReport.header = executionDistributionHeaderLines;
                    simpleReport.resultType = DISTRIBUTION;
                }
                reports.add(simpleReport);

                // comparative mode
                if (definitionMap.size() == 2) {
                    Iterator<CohortDefinitionEntity> iter = definitionMap.values().iterator();
                    var firstCohortDef = iter.next();
                    var secondCohortDef = iter.next();
                    var comparativeResultItem = analysisItem.getComparativeItems(firstCohortDef,
                            secondCohortDef, feAnalysisMap);
                    var comparativeReport = new Report(analysisItem.getName(), analysisId, comparativeResultItem);
                    comparativeReport.header = executionComparativeHeaderLines;
                    comparativeReport.isComparative = true;
                    comparativeReport.faType = analysisItem.getFaType();
                    comparativeReport.domainId = feAnalysisMap.get(analysisItem.getName());
                    if (PREVALENCE.equals(analysisItem.getType())) {
                        comparativeReport.resultType = PREVALENCE;
                        // Summary comparative reports are only available for prevalence type
                        comparativeResultSummary.add(comparativeResultItem);
                    } else if (DISTRIBUTION.equals(analysisItem.getType())) {
                        comparativeReport.resultType = DISTRIBUTION;
                    }
                    reports.add(comparativeReport);
                }
            }
            if (!ignoreSummary) {
                // summary comparative reports are only available for prevalence type
                if (!simpleResultSummary.isEmpty()) {
                    var simpleSummaryData = new Report("All prevalence covariates", simpleResultSummary);
                    simpleSummaryData.header = executionPrevalenceHeaderLines;
                    simpleSummaryData.isSummary = true;
                    simpleSummaryData.resultType = PREVALENCE;
                    reports.add(simpleSummaryData);
                }
                // comparative mode
                if (!comparativeResultSummary.isEmpty()) {
                    var comparativeSummaryData = new Report("All prevalence covariates", comparativeResultSummary);
                    comparativeSummaryData.header = executionComparativeHeaderLines;
                    comparativeSummaryData.isSummary = true;
                    comparativeSummaryData.isComparative = true;
                    comparativeSummaryData.resultType = PREVALENCE;
                    reports.add(comparativeSummaryData);
                }
            }

            return reports;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<CcPrevalenceStat> getPrevalenceStatsByGenerationId(Long id, Long analysisId, Long cohortId,
            Long covariateId) {
        final var generationEntity = ccGenerationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(String.format(GENERATION_NOT_FOUND_ERROR, id)));
        final var source = generationEntity.getSource();
        final var vocabSchema = SourceUtils.getVocabularyQualifier(source);
        final var resultSchema = SourceUtils.getResultsQualifier(source);
        final var tempSchema = SourceUtils.getTempQualifier(source);
        var prevalenceStats = sourceAwareSqlRender.renderSql(source.getSourceId(), queryPrevalenceStatsSql,
                PREVALENCE_STATS_PARAMS,
                new String[] { vocabSchema, resultSchema, String.valueOf(id), String.valueOf(analysisId),
                        String.valueOf(cohortId), String.valueOf(covariateId) });
        var translatedSql = SqlTranslate.translateSql(prevalenceStats, source.getSourceDialect(),
                SessionUtils.sessionId(), tempSchema);
        String[] stmts = SqlSplit.splitSql(translatedSql);
        if (stmts.length == 1) { // Some DBMS like HIVE fails when a single statement ends with dot-comma
            translatedSql = StringUtils.removeEnd(translatedSql.trim(), ";");
        }
        return sourceService.getSourceJdbcTemplate(source).query(translatedSql, (rs, rowNum) -> {
            CcPrevalenceStat stat = new CcPrevalenceStat();
            stat.setAvg(rs.getDouble("stat_value"));
            stat.setConceptId(rs.getLong("concept_id"));
            stat.setConceptName(rs.getString("concept_name"));
            stat.setCount(rs.getLong("count_value"));
            stat.setCovariateId(rs.getLong("covariate_id"));
            stat.setCovariateName(rs.getString("covariate_name"));
            stat.setAnalysisId(rs.getInt("analysis_id"));
            stat.setAnalysisName(rs.getString("analysis_name"));
            stat.setSourceKey(source.getSourceKey());
            stat.setDistance(rs.getInt("min_levels_of_separation"));
            stat.setStrataId(rs.getLong("strata_id"));
            stat.setStrataName(rs.getString("strata_name"));
            stat.setFaType(rs.getString("fa_type"));
            return stat;
        });
    }

    @Override
    public void deleteCcGeneration(Long generationId) {
        final var generationEntity = ccGenerationRepository.findById(generationId)
                .orElseThrow(
                        () -> new IllegalArgumentException(String.format(GENERATION_NOT_FOUND_ERROR, generationId)));
        final var source = generationEntity.getSource();
        final var sql = sourceAwareSqlRender.renderSql(source.getSourceId(), deleteResultsSql, PARAMETERS_RESULTS,
                new String[] { String.valueOf(generationId) });
        final var tempSchema = SourceUtils.getTempQualifier(source);
        final var translatedSql = SqlTranslate.translateSql(sql, source.getSourceDialect(), SessionUtils.sessionId(),
                tempSchema);
        sourceService.getSourceJdbcTemplate(source).execute(translatedSql);

        final var deleteJobSql = sourceAwareSqlRender.renderSql(source.getSourceId(), deleteExecutionSql,
                new String[] { "ohdsiSchema", "execution_id" },
                new String[] { webApiProperties.appDb().schemaName(), String.valueOf(generationId) });
        final var translatedJobSql = SqlTranslate.translateSql(deleteJobSql, "postgres");
        webapiJdbcTemplate.batchUpdate(translatedJobSql.split(";"));
    }

    @Override
    public void cancelGeneration(Long id, String sourceKey) {
        var source = sourceService.findBySourceKey(sourceKey);
        if (Objects.isNull(source)) {
            throw new NotFoundException();
        }
        jobService.cancelJobExecution(j -> {
            var jobParameters = j.getJobParameters();
            var jobName = j.getJobInstance().getJobName();
            return Objects.equals(jobParameters.getString(SOURCE_ID), Integer.toString(source.getSourceId()))
                    && Objects.equals(jobParameters.getString(COHORT_CHARACTERIZATION_ID), Long.toString(id))
                    && Objects.equals(getJobName(), jobName);
        });
    }

    public List<VersionDTO> getVersions(final long id) {
       var versions = versionService.getVersions(id);
        return versions.stream()
                .map(VersionBaseToVersionDTOConverter::convert)
                .collect(Collectors.toList());
    }

    public CcVersionFullDTO getVersion(final long id, final int version) {
        checkVersion(id, version, false);
        var characterizationVersion = versionService.getById(id,
                version);
        return characterizationVersionToFullDTOConverter.convert(characterizationVersion);
    }

    public VersionDTO updateVersion(final long id, final int version,
            VersionUpdateDTO updateDTO) {
        checkVersion(id, version);
        updateDTO.setAssetId(id);
        updateDTO.setVersion(version);
        var updated = versionService.update(updateDTO);

        return VersionToVersionDTOConverter.convert(updated);
    }

    public void deleteVersion(final long id, final int version) {
        checkVersion(id, version);
        versionService.delete(id, version);
    }

    @Transactional
    public CohortCharacterizationEntity copyAssetFromVersion(final long id, final int version) {
        checkVersion(id, version, false);
        var characterizationVersion = versionService.getById(id,
                version);

        var fullDTO = conversionService.convert(characterizationVersion, CcVersionFullDTO.class);
        var entity = conversionService.convert(fullDTO.getEntityDTO(),
                CohortCharacterizationEntity.class);
        entity.setId(null);
        entity.setTags(null);
        entity.setName(NameUtils.getNameForCopy(entity.getName(), this::getNamesLike,
                repository.findByName(entity.getName())));

        var saved = createCc(entity);
        return saved;
    }

    private void checkVersion(long id, int version) {
        checkVersion(id, version, true);
    }

    private void checkVersion(long id, int version, boolean checkOwnerShip) {
        var characterizationVersion = versionService.getById(id, version);
        ExceptionUtils.throwNotFoundExceptionIfNull(characterizationVersion,
                String.format("There is no cohort characterization version with id = %d.", version));

        var entity = findById(id);
        if (checkOwnerShip) {
            authorizationManager.throwIfCurrentUserDoesntOwn(entity);
        }
    }

    public CharacterizationVersion saveVersion(long id) {
        var def = findById(id);
        var version = conversionService.convert(def, CharacterizationVersion.class);

        var user = Objects.nonNull(def.getModifiedBy()) ? def.getModifiedBy() : def.getCreatedBy();
        var versionDate = Objects.nonNull(def.getModifiedDate()) ? def.getModifiedDate() : def.getCreatedDate();
        version.setCreatedBy(user);
        version.setCreatedDate(versionDate);
        return versionService.create(version);
    }

    private List<String> getNamesLike(String copyName) {
        return repository.findAllByNameStartsWith(copyName).stream().map(CohortCharacterizationEntity::getName)
                .collect(Collectors.toList());
    }

    @Override
    public String getJobName() {
        return GENERATE_COHORT_CHARACTERIZATION;
    }

    @Override
    public String getExecutionFoldingKey() {
        return COHORT_CHARACTERIZATION_ID;
    }

    @Override
    public List<ConceptSetExport> exportConceptSets(CohortCharacterization cohortCharacterization) {
        var prioritySource = new SourceInfo(vocabularyService.getPriorityVocabularySource());
        return cohortCharacterization.getStrataConceptSets().stream()
                .map(cs -> vocabularyService.exportConceptSet(cs, prioritySource))
                .collect(Collectors.toList());
    }

    @Override
    public List<CohortCharacterizationEntity> listByTags(TagNameListRequestDTO requestDTO) {
        var names = requestDTO.names().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
        var entities = repository.findByTags(names);
        return tagService.listByTags(entities, names);
    }

    private List<CcResult> getGenerationResults(final SourceEntity source, final String translatedSql) {
        return sourceService.getSourceJdbcTemplate(source).query(translatedSql, (rs, rowNum) -> {
            final var type = rs.getString("type");
            if (StringUtils.equals(type, DISTRIBUTION.toString())) {
                final var distributionStat = new CcDistributionStat();
                gatherForPrevalence(distributionStat, rs);
                gatherForDistribution(distributionStat, rs);
                return distributionStat;
            } else if (StringUtils.equals(type, PREVALENCE.toString())) {
                final var prevalenceStat = new CcPrevalenceStat();
                gatherForPrevalence(prevalenceStat, rs);
                return prevalenceStat;
            }
            return null;
        });
    }

    private void gatherForPrevalence(final CcPrevalenceStat stat, final ResultSet rs) throws SQLException {
        Long generationId = rs.getLong("cc_generation_id");
        var optionalCc = ccGenerationRepository.findById(generationId);
        if (optionalCc.isEmpty()) {
            throw new RuntimeException(String.format(GENERATION_NOT_FOUND_ERROR, generationId));
        }
        var ccGeneration = optionalCc.get();

        stat.setFaType(rs.getString("fa_type"));
        stat.setSourceKey(ccGeneration.getSource().getSourceKey());
        stat.setCohortId(rs.getInt("cohort_definition_id"));
        stat.setAnalysisId(rs.getInt("analysis_id"));
        stat.setAnalysisName(rs.getString("analysis_name"));
        stat.setResultType(PREVALENCE);
        stat.setCovariateId(rs.getLong("covariate_id"));
        stat.setCovariateName(rs.getString("covariate_name"));
        stat.setConceptName(rs.getString("concept_name"));
        stat.setTimeWindow(getTimeWindow(rs.getString("analysis_name")));
        stat.setConceptId(rs.getLong("concept_id"));
        stat.setAvg(rs.getDouble("avg_value"));
        stat.setCount(rs.getLong("count_value"));
        stat.setStrataId(rs.getLong("strata_id"));
        stat.setStrataName(rs.getString("strata_name"));
    }

    private void gatherForDistribution(final CcDistributionStat stat, final ResultSet rs) throws SQLException {
        stat.setResultType(DISTRIBUTION);
        stat.setAvg(rs.getDouble("avg_value"));
        stat.setStdDev(rs.getDouble("stdev_value"));
        stat.setMin(rs.getDouble("min_value"));
        stat.setP10(rs.getDouble("p10_value"));
        stat.setP25(rs.getDouble("p25_value"));
        stat.setMedian(rs.getDouble("median_value"));
        stat.setP75(rs.getDouble("p75_value"));
        stat.setP90(rs.getDouble("p90_value"));
        stat.setMax(rs.getDouble("max_value"));
        stat.setAggregateId(rs.getInt("aggregate_id"));
        stat.setAggregateName(rs.getString("aggregate_name"));
        stat.setMissingMeansZero(rs.getInt("missing_means_zero") == 1);
    }

    public String getTimeWindow(String analysisName) {
        if (analysisName.endsWith("LongTerm"))
            return "Long Term";
        if (analysisName.endsWith("MediumTerm"))
            return "Medium Term";
        if (analysisName.endsWith("ShortTerm"))
            return "Short Term";
        if (analysisName.endsWith("AnyTimePrior"))
            return "Any Time Prior";
        if (analysisName.endsWith("Overlapping"))
            return "Overlapping";

        return "None";
    }

    private List<Integer> importAnalyses(final CohortCharacterizationEntity entity,
            final CohortCharacterizationEntity persistedEntity) {
        var savedAnalysesIds = new ArrayList<Integer>();
        final var presetAnalysesMap = buildPresetAnalysisMap(entity);

        final var analysesSet = new HashSet<FeAnalysisEntity>();

        for (final var newAnalysis : entity.getFeatureAnalyses()) {
            switch (newAnalysis.getType()) {
                case CRITERIA_SET:
                    FeAnalysisWithCriteriaEntity<? extends FeAnalysisCriteriaEntity> criteriaAnalysis = (FeAnalysisWithCriteriaEntity) newAnalysis;
                    List<? extends FeAnalysisCriteriaEntity> design = criteriaAnalysis.getDesign();
                    Optional<FeAnalysisEntity> entityCriteriaSet = analysisService
                            .findByCriteriaListAndCsAndDomainAndStat(
                                    design, criteriaAnalysis);
                    this.<FeAnalysisWithCriteriaEntity<?>>addAnalysis(savedAnalysesIds, analysesSet, criteriaAnalysis,
                            entityCriteriaSet, a -> analysisService.createCriteriaAnalysis(a));
                    break;
                case PRESET:
                    analysesSet.add(presetAnalysesMap.get(newAnalysis.getDesign()));
                    break;
                case CUSTOM_FE:
                    FeAnalysisWithStringEntity withStringEntity = (FeAnalysisWithStringEntity) newAnalysis;
                    Optional<? extends FeAnalysisEntity> curAnalysis = analysisService.findByDesignAndName(
                            withStringEntity, withStringEntity.getName());
                    this.<FeAnalysisEntity>addAnalysis(savedAnalysesIds, analysesSet, newAnalysis, curAnalysis,
                            a -> analysisService.createAnalysis(a));
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Analysis with type: " + newAnalysis.getType() + " cannot be imported");
            }
        }

        persistedEntity.setFeatureAnalyses(analysesSet);
        return savedAnalysesIds;
    }

    private <T extends FeAnalysisEntity<?>> void addAnalysis(List<Integer> savedAnalysesIds,
            Set<FeAnalysisEntity> entityAnalyses, T newAnalysis,
            Optional<? extends FeAnalysisEntity> curAnalysis, Function<T, FeAnalysisEntity> func) {
        if (curAnalysis.isPresent()) {
            entityAnalyses.add(curAnalysis.get());
        } else {
            newAnalysis.setName(NameUtils.getNameWithSuffix(newAnalysis.getName(), this::getFeNamesLike));
            var created = func.apply(newAnalysis);
            entityAnalyses.add(created);
            savedAnalysesIds.add(created.getId());
        }
    }

    private Map<String, FeAnalysisEntity> buildPresetAnalysisMap(final CohortCharacterizationEntity entity) {
        return analysisService
                .findPresetAnalysesBySystemNames(gatherPresetAnalyses(entity))
                .stream()
                .collect(Collectors.toMap(FeAnalysisEntity::getDesign, Function.identity()));
    }

    private List<String> gatherPresetAnalyses(final CohortCharacterizationEntity entity) {
        return entity.getFeatureAnalyses()
                .stream()
                .filter(a -> StandardFeatureAnalysisType.PRESET.equals(a.getType()))
                .map(FeAnalysisEntity::getDesign)
                .map(v -> (String) v)
                .collect(Collectors.toList());
    }

    private void importCohorts(final CohortCharacterizationEntity entity,
            final CohortCharacterizationEntity persistedEntity) {
        final var cohortList = entity.getCohortDefinitions().stream()
                .map(designImportService::persistCohortOrGetExisting)
                .collect(Collectors.toSet());
        persistedEntity.setCohortDefinitions(cohortList);
    }

    private void cleanIds(final CohortCharacterizationEntity entity) {
        entity.setId(null);
        entity.getParameters().forEach(v -> v.setId(null));
        entity.getCohortDefinitions().forEach(v -> v.setId(null));
        entity.getFeatureAnalyses().forEach(v -> v.setId(null));
    }

    private Map<String, CcParamEntity> buildParamNameToParamMap(final CohortCharacterizationEntity foundEntity) {
        return foundEntity.getParameters()
                .stream()
                .collect(Collectors.toMap(CcParamEntity::getName, Function.identity()));
    }

    @PostConstruct
    public void init() {
        invalidateGenerations();
    }

    private void invalidateGenerations() {
        transactionTemplates.getTransactionTemplateRequiresNew().execute((TransactionStatus transactionStatus) -> {
            var generations = findAllIncompleteGenerations();
            generations.forEach(generation -> {
                JobExecution job = jobService.getJobExecution(generation.getId());
                jobService.invalidateJobExecution(job);
            });
            return null;
        });
    }

    private List<String> getFeNamesLike(String name) {
        return analysisService.getNamesLike(name);
    }
}
