package org.ohdsi.webapi.transactiontemplates;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TransactionTemplates {
    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate transactionTemplateRequiresNew;
    private final TransactionTemplate transactionTemplateNoTransaction;

    public TransactionTemplates(PlatformTransactionManager transactionManager) {
        transactionTemplate = new TransactionTemplate();
        transactionTemplate.setTransactionManager(transactionManager);

        transactionTemplateRequiresNew = new TransactionTemplate();
        transactionTemplateRequiresNew.setTransactionManager(transactionManager);
        transactionTemplateRequiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transactionTemplateNoTransaction = new TransactionTemplate();
        transactionTemplateNoTransaction.setTransactionManager(transactionManager);
        transactionTemplateNoTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    }

    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    public TransactionTemplate getTransactionTemplateRequiresNew() {
        return transactionTemplateRequiresNew;
    }

    public TransactionTemplate getTransactionTemplateNoTransaction() {
        return transactionTemplateNoTransaction;
    }
}
