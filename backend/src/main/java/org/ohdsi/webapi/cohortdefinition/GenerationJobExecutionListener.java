/*
 * Copyright 2017 cknoll1.
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

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.Constants;
import org.ohdsi.webapi.generationtasks.GenerationStatus;
import org.ohdsi.webapi.source.SourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.ws.rs.NotFoundException;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static org.ohdsi.webapi.Constants.Params.TARGET_DATABASE_SCHEMA;

public class GenerationJobExecutionListener implements JobExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(GenerationJobExecutionListener.class);
    private final SourceService sourceService;
    private final CohortDefinitionRepository cohortDefinitionRepository;
    private final TransactionTemplate transactionTemplate;
    private final JdbcTemplate sourceTemplate;

    public GenerationJobExecutionListener(SourceService sourceService,
            CohortDefinitionRepository cohortDefinitionRepository,
            TransactionTemplate transactionTemplate,
            JdbcTemplate sourceTemplate) {
        this.sourceService = sourceService;
        this.cohortDefinitionRepository = cohortDefinitionRepository;
        this.transactionTemplate = transactionTemplate;
        this.sourceTemplate = sourceTemplate;
    }

    private CohortGenerationInfo findBySourceId(CohortDefinitionEntity df, Integer sourceId) {
        return df.getGenerationInfoList().stream()
                .filter(info -> info.getId().getSourceId().equals(sourceId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        String.format("Cannot find cohortGenerationInfo for cohortDefinition[{%d}] sourceId[%s]",
                                df.getId(), sourceId)));
    }

    @Override
    public void afterJob(JobExecution je) {
        var jobParams = je.getJobParameters();
        var defId = Integer.valueOf(jobParams.getString("cohort_definition_id"));
        var sourceId = Integer.valueOf(jobParams.getString("source_id"));
        var cohortTable = jobParams.getString(TARGET_DATABASE_SCHEMA) + ".cohort";

        var completeTx = new DefaultTransactionDefinition();
        completeTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        var completeStatus = this.transactionTemplate.getTransactionManager().getTransaction(completeTx);

        try {
            var source = sourceService.findBySourceId(sourceId);

            var fieldValue = this.cohortDefinitionRepository.findById(defId);
            if (fieldValue.isEmpty()) {
                throw new NotFoundException("Cannot find cohort definition %d".formatted(defId));
            }
            var df = fieldValue.get();
            var info = findBySourceId(df, sourceId);
            setExecutionDurationIfPossible(je, info);
            info.setStatus(GenerationStatus.COMPLETE);

            if (je.getStatus() == BatchStatus.FAILED || je.getStatus() == BatchStatus.STOPPED) {
                info.setIsValid(false);
                info.setRecordCount(null);
                info.setPersonCount(null);
                info.setCanceled(je.getStepExecutions().stream()
                        .anyMatch(se -> Objects.equals(Constants.CANCELED, se.getExitStatus().getExitCode())));
                info.setFailMessage(StringUtils.abbreviateMiddle(je.getAllFailureExceptions().getFirst().getMessage(),
                        "... [truncated] ...", 2000));
            } else {
                info.setIsValid(true);
                info.setFailMessage(null);

                // query summary results from source
                var statsQuery = "SELECT count(distinct subject_id) as person_count, count(*) as record_count from @cohort_table where cohort_definition_id = @cohort_definition_id";
                var statsSql = SqlTranslate.translateSql(statsQuery, source.getSourceDialect(), null, null);
                var renderedSql = SqlRender.renderSql(statsSql,
                        new String[] { "cohort_table", "cohort_definition_id" },
                        new String[] { cohortTable, defId.toString() });
                var stats = this.sourceTemplate.queryForMap(renderedSql);
                info.setPersonCount(Long.parseLong(stats.get("person_count").toString()));
                info.setRecordCount(Long.parseLong(stats.get("record_count").toString()));
            }

            this.cohortDefinitionRepository.save(df);
            this.transactionTemplate.getTransactionManager().commit(completeStatus);
        } catch (Exception e) {
            this.transactionTemplate.getTransactionManager().rollback(completeStatus);
            log.error(e.getMessage());
            je.addFailureException(e);
            je.setExitStatus(new ExitStatus("FAILED", "Could not complete cohort generation job"));
            je.setStatus(BatchStatus.FAILED);
        }
    }

    private void setExecutionDurationIfPossible(JobExecution je, CohortGenerationInfo info) {
        if (Objects.isNull(je.getEndTime()) || Objects.isNull(je.getStartTime())) {
            log.error(
                    "Cannot set duration time for cohortGenerationInfo[{}]. startData[{}] and endData[{}] cannot be empty.",
                    info.getId(), je.getStartTime(), je.getEndTime());
            return;
        }
        info.setExecutionDuration((int) (Date.from(je.getEndTime().atZone(ZoneId.systemDefault()).toInstant()).getTime() - Date.from(je.getStartTime().atZone(ZoneId.systemDefault()).toInstant()).getTime()));
    }

    @Override
    public void beforeJob(JobExecution je) {
        var startTime = Calendar.getInstance().getTime();
        var jobParams = je.getJobParameters();
        var defId = Integer.valueOf(jobParams.getString("cohort_definition_id"));
        var sourceId = Integer.valueOf(jobParams.getString("source_id"));

        var initTx = new DefaultTransactionDefinition();
        initTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        var initStatus = this.transactionTemplate.getTransactionManager().getTransaction(initTx);
        try {
            var fieldValue = this.cohortDefinitionRepository.findById(defId);
            if (fieldValue.isEmpty()) {
                throw new NotFoundException("Cannot find cohort definition %d".formatted(defId));
            }
            var df = fieldValue.get();

            var info = findBySourceId(df, sourceId);
            info.setIsValid(false);
            info.setStartTime(startTime);
            info.setStatus(GenerationStatus.RUNNING);
            this.cohortDefinitionRepository.save(df);
            this.transactionTemplate.getTransactionManager().commit(initStatus);
        } catch (Exception e) {
            this.transactionTemplate.getTransactionManager().rollback(initStatus);
            log.error(e.getMessage());
            je.addFailureException(e);
            je.setExitStatus(new ExitStatus("FAILED", "Could not start cohort generation job"));
            je.setStatus(BatchStatus.FAILED);
        }
    }
}
