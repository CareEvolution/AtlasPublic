package org.ohdsi.webapi.jpa;

import jakarta.validation.constraints.Positive;

public class JdbcProperties {
    @Positive(message = "jdbc.batch_size must be >= 0")
    private Integer batchSize;

    public Integer getBatchSize() {
        return batchSize;
    }
    public Integer batchSize() {
        return batchSize;
    }
    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }
}
