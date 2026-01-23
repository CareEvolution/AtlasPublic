package org.ohdsi.webapi.cohortdefinition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.ohdsi.webapi.cohortdefinition.CohortDefinitionController.GenerateSqlRequest;
import org.ohdsi.webapi.cohortdefinition.converter.CohortDefinitionToCohortVersionConverter;
import org.ohdsi.webapi.cohortdefinition.dto.CohortDTO;
import org.ohdsi.webapi.cohortdefinition.event.CohortDefinitionChangedEvent;
import org.ohdsi.webapi.cohortsample.CohortSamplingService;
import org.ohdsi.webapi.common.SourceMapKey;
import org.ohdsi.webapi.common.generation.GenerateSqlResult;
import org.ohdsi.webapi.common.sensitiveinfo.CohortGenerationSensitiveInfoService;
import org.ohdsi.webapi.conceptset.ConceptSetExport;
import org.ohdsi.webapi.job.JobService;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.source.SourceDaimonEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceInfo;
import org.ohdsi.webapi.source.SourceService;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.analysis.Utils;
import org.ohdsi.circe.cohortdefinition.CohortExpression;
import org.ohdsi.circe.cohortdefinition.CohortExpressionQueryBuilder;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.webapi.Constants;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.converter.UserDtoToUserEntity;
import org.ohdsi.webapi.authorization.model.SecuredEntityType;
import org.ohdsi.webapi.tag.TagService;
import org.ohdsi.webapi.tag.dto.TagNameListRequestDTO;
import org.ohdsi.webapi.transactiontemplates.TransactionTemplates;
import org.ohdsi.webapi.util.ExceptionUtils;
import org.ohdsi.webapi.util.NameUtils;
import org.ohdsi.webapi.util.PreparedStatementRenderer;
import org.ohdsi.webapi.util.SessionUtils;
import org.ohdsi.webapi.versioning.domain.CohortVersion;
import org.ohdsi.webapi.versioning.service.CohortDefinitionVersionService;
import org.ohdsi.webapi.vocabulary.VocabularyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.NotFoundException;

import static org.ohdsi.webapi.Constants.Params.*;
import static org.ohdsi.webapi.util.SecurityUtils.whitelist;

@Service
public class CohortDefinitionService {
    private static final Logger log = LoggerFactory.getLogger(CohortDefinitionService.class);
    private static final CohortExpressionQueryBuilder queryBuilder = new CohortExpressionQueryBuilder();

    private final AuthorizationManager authorizationManager;
    private final TransactionTemplates transactionTemplates;
    private final CohortDefinitionRepository cohortDefinitionRepository;
    private final ConversionService conversionService;
    private final TagService tagService;
    private final ApplicationEventPublisher eventPublisher;
    private final CohortDefinitionVersionService versionService;
    private final SourceService sourceService;
    private final CohortGenerationSensitiveInfoService sensitiveInfoService;
    private final JobService jobService;
    private final StringResourceReader resourceReader;
    private final CohortSamplingService samplingService;
    private final ObjectMapper objectMapper;
    private final VocabularyService vocabularyService;

    public CohortDefinitionService(AuthorizationManager authorizationManager,
        TransactionTemplates transactionTemplates, 
        CohortDefinitionRepository cohortDefinitionRepository,
        ConversionService conversionService,
        TagService tagService,
        ApplicationEventPublisher eventPublisher,
        CohortDefinitionVersionService versionService,
        SourceService sourceService,
        CohortGenerationSensitiveInfoService sensitiveInfoService,
        JobService jobService,
        StringResourceReader resourceReader,
        CohortSamplingService samplingService,
        ObjectMapper objectMapper,
        VocabularyService vocabularyService) {
        this.authorizationManager = authorizationManager;
        this.transactionTemplates = transactionTemplates;
        this.cohortDefinitionRepository = cohortDefinitionRepository;
        this.conversionService = conversionService;
        this.tagService = tagService;
        this.eventPublisher = eventPublisher;
        this.versionService = versionService;
        this.sourceService = sourceService;
        this.sensitiveInfoService = sensitiveInfoService;
        this.jobService = jobService;
        this.resourceReader = resourceReader;
        this.samplingService = samplingService;
        this.objectMapper = objectMapper;
        this.vocabularyService = vocabularyService;
    }

    public List<CohortDefinitionEntity> getAllCohortDefinitions() {
        var definitions = cohortDefinitionRepository.list();
        return definitions;
    }

    public CohortDefinitionEntity getCohortDefinition(final int id) {
        var optionalDef = cohortDefinitionRepository.findById(id);
        if (optionalDef.isEmpty()) {
            throw new NotFoundException(String.format("There is no cohort definition with id = %d.", id));
        }
        return optionalDef.get();
    }

    public CohortDefinitionEntity getCohortDefinitionWithDetail(final int id) {
        var def = this.cohortDefinitionRepository.findOneWithDetail(id);
        return def;
    }

    public Optional<CohortDefinitionEntity> findByName(final String name) {
        return cohortDefinitionRepository.findByName(name);
    }

    public CohortDTO getCohortDefinitionDto(final int id) {
        return transactionTemplates.getTransactionTemplate().execute(transactionStatus -> {
            var d = this.cohortDefinitionRepository.findOneWithDetail(id);
            ExceptionUtils.throwNotFoundExceptionIfNull(d,
                    String.format("There is no cohort definition with id = %d.", id));
            var dto = conversionService.convert(d, CohortDTO.class);
            authorizationManager.fillPermissions(d, dto);
            return dto;
        });
    }

    public List<String> getNamesLike(String copyName) {
        return cohortDefinitionRepository.findAllByNameStartsWith(copyName).stream().map(CohortDefinitionEntity::getName)
                .collect(Collectors.toList());
    }

    public int getCountCDefWithSameName(final int id, String name) {
        return cohortDefinitionRepository.getCountCDefWithSameName(id, name);
    }

    public List<CohortDefinitionEntity> getCohortDefinitions(List<Integer> ids) {
        var defs = new ArrayList<CohortDefinitionEntity>();
        for (var id : ids) {
            var optionalDef = cohortDefinitionRepository.findById(id);
            optionalDef.ifPresent(defs::add);
        }
        return defs;
    }

    public void assignTag(final Integer id, final int tagId) {
        var optEntity = cohortDefinitionRepository.findById(id);
        if (optEntity.isEmpty()) {
            throw new NotFoundException(String.format("There is no cohort definition with id = %d.", id));
        }
        var entity = optEntity.get();
        tagService.assignTag(entity, tagId);
    }

    public void assignTag(List<Integer> ids, final int tagId) {
        ids.forEach(id -> {
            try {
                assignTag(id, tagId);
            } catch (final AccessDeniedException e) {
                log.warn("Tag {} cannot be assigned to entity {} - forbidden", tagId, id);
            }
        });
    }

    public void unassignTag(final Integer id, final int tagId) {
        var optionalEntity = cohortDefinitionRepository.findById(id);
        if (optionalEntity.isEmpty()) {
            throw new NotFoundException(String.format("There is no cohort definition with id = %d.", id));
        }
        var entity = optionalEntity.get();
        tagService.unassignTag(entity, tagId);
    }

    public void unassignTag(List<Integer> ids, final int tagId) {
        ids.forEach(id -> {
            try {
                unassignTag(id, tagId);
            } catch (final AccessDeniedException e) {
                log.warn("Tag {} cannot be unassigned from entity {} - forbidden", tagId, id);
            }
        });
    }

    public List<CohortDefinitionEntity> listByTags(TagNameListRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.names() == null || requestDTO.names().isEmpty()) {
            return Collections.emptyList();
        }

        var names = requestDTO.names().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());

        var entities = cohortDefinitionRepository.findByTags(names);

        return tagService.listByTags(entities, names);
    }

    public List<CohortGenerationInfo> getInfo(final int id) {
        var cohortDefinition = this.cohortDefinitionRepository.findById(id)
          .orElseThrow(() -> new NotFoundException(String.format("There is no cohort definition with id = %d.", id)));
        
        var generationInfoList = cohortDefinition.getGenerationInfoList();

        var sourceMap = sourceService.getSourcesMap(SourceMapKey.BY_SOURCE_ID);
        var result = generationInfoList.stream()
                .filter(genInfo -> {
                    var sourceId = genInfo.getId().getSourceId();
                    var source = sourceMap.get(sourceId);
                    if (source == null) {
                        return false;
                    }
                    var sourceKey = source.getSourceKey();
                    return authorizationManager.currentUserCanRead(sourceKey);
                })
                .collect(Collectors.toList());

        var filteredResult = sensitiveInfoService.filterSensitiveInfo(result,
                gi -> Collections.singletonMap(Constants.Variables.SOURCE, sourceMap.get(gi.getId().getSourceId())));
        return filteredResult;
    }

    public List<ConceptSetExport> getConceptSetExports(CohortDefinitionEntity def, SourceInfo vocabSource)
            throws RuntimeException {
        CohortExpression expression;
        try {
            expression = objectMapper.readValue(def.getDetails().getExpression(), CohortExpression.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Arrays.stream(expression.conceptSets)
                .map(cs -> vocabularyService.exportConceptSet(cs, vocabSource))
                .collect(Collectors.toList());
    }

    public InclusionRuleReport getInclusionRuleReport(final int id, final String sourceKey, int modeId) {
        var source = sourceService.findBySourceKey(sourceKey);

        var summary = getInclusionRuleReportSummary(whitelist(id), source, modeId);
        var inclusionRuleStats = getInclusionRuleStatistics(whitelist(id), source, modeId);
        var treemapData = getInclusionRuleTreemapData(whitelist(id), inclusionRuleStats.size(), source, modeId);

        var report = new InclusionRuleReport();
        report.summary = summary;
        report.inclusionRuleStats = inclusionRuleStats;
        report.treemapData = treemapData;

        return report;
    }

    public GenerateSqlResult generateSql(GenerateSqlRequest request) {
        var options = request.options;
        if (options == null) {
            options = new CohortExpressionQueryBuilder.BuildExpressionQueryOptions();
        }

        var result = new GenerateSqlResult();
        var expressionSql = queryBuilder.buildExpressionQuery(request.expression, options);
        result.templateSql = SqlRender.renderSql(expressionSql, null, null);
        return result;
    }

    public CohortDefinitionEntity createCohortDefinition(CohortDTO dto) {
        var currentTime = Calendar.getInstance().getTime();
        var currentUser = authorizationManager.getCurrentUser();

        // Create definition in 2 saves, first to get the generated ID for the new dto
        // then to associate the details with the definition
        var newDef = new CohortDefinitionEntity();
        newDef.setName(StringUtils.trim(dto.getName()))
                .setDescription(dto.getDescription())
                .setExpressionType(dto.getExpressionType());
        newDef.setCreatedBy(UserDtoToUserEntity.convert(currentUser));
        newDef.setCreatedDate(currentTime);
        newDef = this.cohortDefinitionRepository.save(newDef);

        // Associate details
        var details = new CohortDefinitionDetails();
        details.setCohortDefinition(newDef)
                .setExpression(Utils.serialize(dto.getExpression()));

        newDef.setDetails(details);

        var createdDefinition = this.cohortDefinitionRepository.save(newDef);
        authorizationManager.addOwnership(createdDefinition);
        return createdDefinition;
    }

    public CohortDefinitionEntity copyCohortDefinition(final int id) {
        // We get this as a DTO so that we get a fresh object that is detached from the session
        var sourceDef = getCohortDefinitionDto(id);
        sourceDef.setId(null); // clear the ID
        sourceDef.setTags(null);
        sourceDef.setName(NameUtils.getNameForCopy(sourceDef.getName(), this::getNamesLike,
                cohortDefinitionRepository.findByName(sourceDef.getName())));
        return createCohortDefinition(sourceDef);
    }

    public CohortDefinitionEntity saveCohortDefinition(final int id, CohortDTO def) {
        var currentTime = Calendar.getInstance().getTime();
        saveVersion(id);

        var currentDefinition = this.cohortDefinitionRepository.findOneWithDetail(id);
        var currentUser = authorizationManager.getCurrentUser();

        currentDefinition.setName(def.getName())
                .setDescription(def.getDescription())
                .setExpressionType(def.getExpressionType())
                .getDetails().setExpression(Utils.serialize(def.getExpression()));
        currentDefinition.setModifiedBy(UserDtoToUserEntity.convert(currentUser));
        currentDefinition.setModifiedDate(currentTime);
        currentDefinition = this.cohortDefinitionRepository.save(currentDefinition);

        eventPublisher.publishEvent(new CohortDefinitionChangedEvent(currentDefinition));

        return currentDefinition;
    }

    public void deleteCohortDefinition(final int id) {
        // perform the JPA update in a separate transaction
        transactionTemplates.getTransactionTemplateRequiresNew().execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                var optionalDef = cohortDefinitionRepository.findById(id);
                if (optionalDef.isPresent()) {
                    var def = optionalDef.get();
                    def.getGenerationInfoList().forEach(cohortGenerationInfo -> {
                        var sourceId = cohortGenerationInfo.getId().getSourceId();

                        jobService.cancelJobExecution(e -> {
                            var parameters = e.getJobParameters();
                            var jobName = e.getJobInstance().getJobName();
                            return Objects.equals(parameters.getString(COHORT_DEFINITION_ID), Integer.toString(id))
                                    && Objects.equals(parameters.getString(SOURCE_ID), Integer.toString(sourceId))
                                    && Objects.equals(Constants.GENERATE_COHORT, jobName);
                        });
                    });
                    cohortDefinitionRepository.delete(def);
                    authorizationManager.removeOwnership(SecuredEntityType.COHORT_DEFINITION, id);
                } else {
                    log.warn("Failed to delete Cohort Definition with ID = {}", id);
                }
            }
        });

        var builder = new JobParametersBuilder();
        builder.addString(JOB_NAME, String.format("Cleanup cohort %d.", id));
        builder.addString(COHORT_DEFINITION_ID, ("" + id));
        final var jobParameters = builder.toJobParameters();

        log.info("Beginning cohort cleanup for cohort definition id: {}", "" + id);

        var cleanupCohortTasklet = new CleanupCohortTasklet(
            sourceService,
            this.resourceReader,
            transactionTemplates.getTransactionTemplateNoTransaction()
        );
        var cleanupSamplesTasklet = samplingService.createDeleteSamplesTasklet();

        var cleanupCohortStep = this.jobService.buildTaskletStep("cohortDefinition.cleanupCohort", cleanupCohortTasklet);
        var cleanupSamplesStep = this.jobService.buildTaskletStep("cohortDefinition.cleanupSamples", cleanupSamplesTasklet);
        var job = this.jobService.buildJob("cleanupCohort", List.of(cleanupCohortStep, cleanupSamplesStep));
        this.jobService.runJob(job, jobParameters);
    }

    private CohortVersion saveVersion(int id) {
        var def = this.cohortDefinitionRepository.findOneWithDetail(id);
        var version = CohortDefinitionToCohortVersionConverter.convert(def);

        var user = Objects.nonNull(def.getModifiedBy()) ? def.getModifiedBy() : def.getCreatedBy();
        var versionDate = Objects.nonNull(def.getModifiedDate()) ? def.getModifiedDate() : def.getCreatedDate();
        version.setCreatedBy(user);
        version.setCreatedDate(versionDate);
        return versionService.create(version);
    }

    private final RowMapper<InclusionRuleReport.Summary> summaryMapper = (rs, rowNum) -> {
        InclusionRuleReport.Summary summary = new InclusionRuleReport.Summary();
        summary.baseCount = rs.getLong("base_count");
        summary.finalCount = rs.getLong("final_count");
        summary.lostCount = rs.getLong("lost_count");

        double matchRatio = (summary.baseCount > 0) ? ((double) summary.finalCount / (double) summary.baseCount) : 0.0;
        summary.percentMatched = new BigDecimal(matchRatio * 100.0).setScale(2, RoundingMode.HALF_UP)
                .toPlainString() + "%";
        return summary;
    };

    private InclusionRuleReport.Summary getInclusionRuleReportSummary(int id, SourceEntity source, int modeId) {
        var sql = "select cs.base_count, cs.final_count, cc.lost_count from @tableQualifier.cohort_summary_stats cs left join @tableQualifier.cohort_censor_stats cc "
                + "on cc.cohort_definition_id = cs.cohort_definition_id where cs.cohort_definition_id = @id and cs.mode_id = @modeId";
        var tqName = "tableQualifier";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        String[] varNames = { "id", "modeId" };
        Object[] varValues = { whitelist(id), whitelist(modeId) };
        var psr = new PreparedStatementRenderer(source, sql, tqName, tqValue, varNames, varValues,
                SessionUtils.sessionId());
        var result = sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(),
                summaryMapper);
        return result.isEmpty() ? new InclusionRuleReport.Summary() : result.getFirst();
    }

    private List<InclusionRuleReport.InclusionRuleStatistic> getInclusionRuleStatistics(
            int id,
            SourceEntity source,
            int modeId) {

        var sql = "select i.rule_sequence, i.name, s.person_count, s.gain_count, s.person_total"
                + " from @tableQualifier.cohort_inclusion i join @tableQualifier.cohort_inclusion_stats s on i.cohort_definition_id = s.cohort_definition_id"
                + " and i.rule_sequence = s.rule_sequence"
                + " where i.cohort_definition_id = @id and mode_id = @modeId ORDER BY i.rule_sequence";
        var tqName = "tableQualifier";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        String[] varNames = { "id", "modeId" };
        Object[] varValues = { whitelist(id), whitelist(modeId) };
        var psr = new PreparedStatementRenderer(source, sql, tqName, tqValue, varNames, varValues,
                SessionUtils.sessionId());
        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(),
                inclusionRuleStatisticMapper);
    }

    private final RowMapper<InclusionRuleReport.InclusionRuleStatistic> inclusionRuleStatisticMapper = (rs, rowNum) -> {
        InclusionRuleReport.InclusionRuleStatistic statistic = new InclusionRuleReport.InclusionRuleStatistic();
        statistic.id = rs.getInt("rule_sequence");
        statistic.name = rs.getString("name");
        statistic.countSatisfying = rs.getLong("person_count");
        long personTotal = rs.getLong("person_total");

        long gainCount = rs.getLong("gain_count");
        double excludeRatio = personTotal > 0 ? (double) gainCount / (double) personTotal : 0.0;
        var percentExcluded = new BigDecimal(excludeRatio * 100.0).setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
        statistic.percentExcluded = percentExcluded + "%";

        long satisfyCount = rs.getLong("person_count");
        double satisfyRatio = personTotal > 0 ? (double) satisfyCount / (double) personTotal : 0.0;
        var percentSatisfying = new BigDecimal(satisfyRatio * 100.0).setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
        statistic.percentSatisfying = percentSatisfying + "%";
        return statistic;
    };

    private String getInclusionRuleTreemapData(int id, int inclusionRuleCount, SourceEntity source, int modeId) {
        var sql = "select inclusion_rule_mask, person_count from @tableQualifier.cohort_inclusion_result where cohort_definition_id = @id and mode_id = @modeId";
        var tqName = "tableQualifier";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        String[] varNames = { "id", "modeId" };
        Object[] varValues = { whitelist(id), whitelist(modeId) };
        var psr = new PreparedStatementRenderer(source, sql, tqName, tqValue, varNames, varValues,
                SessionUtils.sessionId());

        // [0] is the inclusion rule bitmask, [1] is the count of the match
        var items = sourceService.getSourceJdbcTemplate(source)
                .query(psr.getSql(), psr.getSetter(), inclusionRuleResultItemMapper);
        var groups = new HashMap<Integer, List<Long[]>>();
        for (var item : items) {
            int bitsSet = countSetBits(item[0]);
            if (!groups.containsKey(bitsSet)) {
                groups.put(bitsSet, new ArrayList<>());
            }
            groups.get(bitsSet).add(item);
        }

        var treemapData = new StringBuilder("{\"name\" : \"Everyone\", \"children\" : [");

        var groupKeys = new ArrayList<>(groups.keySet());
        Collections.sort(groupKeys);
        Collections.reverse(groupKeys);

        int groupCount = 0;
        // create a nested treemap data where more matches (more bits set in string) appear higher in the hierarchy
        for (var groupKey : groupKeys) {
            if (groupCount > 0) {
                treemapData.append(",");
            }

            treemapData.append(String.format("{\"name\" : \"Group %d\", \"children\" : [", groupKey));

            int groupItemCount = 0;
            for (var groupItem : groups.get(groupKey)) {
                if (groupItemCount > 0) {
                    treemapData.append(",");
                }

                //sb_treemap.Append("{\"name\": \"" + cohort_identifer + "\", \"size\": " + cohorts[cohort_identifer].ToString() + "}");
                treemapData.append(String.format("{\"name\": \"%s\", \"size\": %d}",
                        formatBitMask(groupItem[0], inclusionRuleCount), groupItem[1]));
                groupItemCount++;
            }
            groupCount++;
        }

        treemapData.append(StringUtils.repeat("]}", groupCount + 1));
        return treemapData.toString();
    }

    private final RowMapper<Long[]> inclusionRuleResultItemMapper = (rs, rowNum) -> {
        Long[] resultItem = new Long[2];
        resultItem[0] = rs.getLong("inclusion_rule_mask");
        resultItem[1] = rs.getLong("person_count");
        return resultItem;
    };

    private int countSetBits(long n) {
        int count = 0;
        while (n > 0) {
            n &= (n - 1);
            count++;
        }
        return count;
    }

    private String formatBitMask(Long n, int size) {
        return StringUtils.reverse(StringUtils.leftPad(Long.toBinaryString(n), size, "0"));
    }

}
