package org.ohdsi.webapi.common.generation;

import org.ohdsi.webapi.cohortcharacterization.repository.AnalysisGenerationInfoEntityRepository;
import org.ohdsi.webapi.exceptions.FatalException;
import org.ohdsi.webapi.authorization.entity.UserEntity;
import org.ohdsi.webapi.util.CancelableJdbcTemplate;
import org.slf4j.Logger;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class AnalysisTasklet extends CancelableTasklet {
    protected final AnalysisGenerationInfoEntityRepository analysisGenerationInfoEntityRepository;

    public AnalysisTasklet(Logger log,
            CancelableJdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            AnalysisGenerationInfoEntityRepository analysisGenerationInfoEntityRepository) {

        super(log, jdbcTemplate, transactionTemplate);
        this.analysisGenerationInfoEntityRepository = analysisGenerationInfoEntityRepository;
    }

    protected void saveInfoWithinTheSeparateTransaction(Long jobId, String serializedDesign, UserEntity userEntity) {
        var txDefinition = new DefaultTransactionDefinition();
        txDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus infoSaveTx = null;
        try {
            infoSaveTx = this.transactionTemplate.getTransactionManager().getTransaction(txDefinition);
            saveInfo(jobId, serializedDesign, userEntity);
            this.transactionTemplate.getTransactionManager().commit(infoSaveTx);
        } catch (Exception ex) {
            log.error("Cannot save sourceInfo for the job: {} ", jobId, ex);
            this.transactionTemplate.getTransactionManager().rollback(infoSaveTx);
            throw new FatalException(ex);
        }
    }

    private void saveInfo(Long jobId, String serializedDesign, UserEntity userEntity) {
        var generationInfoEntity = new AnalysisGenerationInfoEntity();
        generationInfoEntity.setId(jobId);
        generationInfoEntity.setDesign(serializedDesign);
        generationInfoEntity.setCreatedBy(userEntity);
        analysisGenerationInfoEntityRepository.save(generationInfoEntity);
    }
}
