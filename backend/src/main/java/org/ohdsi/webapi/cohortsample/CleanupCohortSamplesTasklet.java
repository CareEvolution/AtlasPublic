package org.ohdsi.webapi.cohortsample;

import org.ohdsi.webapi.source.SourceDaimonEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.util.PreparedStatementRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.ohdsi.webapi.Constants.Params.*;

public class CleanupCohortSamplesTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(CleanupCohortSamplesTasklet.class);

    private final TransactionTemplate transactionTemplate;
    private final CohortSampleRepository sampleRepository;
    private final SourceService sourceService;

    public CleanupCohortSamplesTasklet(
            final TransactionTemplate transactionTemplate,
            CohortSampleRepository sampleRepository,
            SourceService sourceService) {
        this.transactionTemplate = transactionTemplate;
        this.sampleRepository = sampleRepository;
        this.sourceService = sourceService;
    }

    private Integer doTask(ChunkContext chunkContext) {
        var jobParams = chunkContext.getStepContext().getJobParameters();
        int cohortDefinitionId = Integer.parseInt(jobParams.get(COHORT_DEFINITION_ID).toString());

        if (jobParams.containsKey(SOURCE_ID)) {
            int sourceId = Integer.parseInt(jobParams.get(SOURCE_ID).toString());
            var sourceEntity = this.sourceService.findBySourceId(sourceId);
            return sourceEntity == null ? 0 : mapSource(sourceEntity, cohortDefinitionId);
        } else {
            return this.sourceService.findAll().stream()
                    .filter(source -> source.getDaimons()
                            .stream()
                            .anyMatch(daimon -> daimon.getDaimonType() == SourceDaimonEntity.DaimonType.Results))
                    .mapToInt(source -> mapSource(source, cohortDefinitionId))
                    .sum();
        }
    }

    private int mapSource(SourceEntity source, int cohortDefinitionId) {
        try {
            var resultSchema = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
            return transactionTemplate.execute(transactionStatus -> {
                var samples = sampleRepository.findByCohortDefinitionIdAndSourceId(cohortDefinitionId,
                        source.getId());
                if (samples.isEmpty()) {
                    return 0;
                }

                sampleRepository.deleteAll(samples);

                int[] cohortSampleIds = samples.stream()
                        .mapToInt(CohortSample::getId)
                        .toArray();

                var renderer = new PreparedStatementRenderer(
                        source,
                        "cohortsample/sql/deleteSampleElementsById.sql",
                        "results_schema",
                        resultSchema,
                        "cohortSampleId",
                        cohortSampleIds);

                sourceService.getSourceJdbcTemplate(source)
                        .update(renderer.getSql(), renderer.getOrderedParams());
                return cohortSampleIds.length;
            });
        } catch (Exception e) {
            log.error("Error deleting samples for cohort: {}, cause: {}", cohortDefinitionId, e.getMessage());
            return 0;
        }
    }

    @Override
    public RepeatStatus execute(final StepContribution contribution, final ChunkContext chunkContext) throws Exception {
        this.transactionTemplate.execute(status -> doTask(chunkContext));

        return RepeatStatus.FINISHED;
    }
}
