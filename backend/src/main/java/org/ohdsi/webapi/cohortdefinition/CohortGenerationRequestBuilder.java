package org.ohdsi.webapi.cohortdefinition;

import org.ohdsi.circe.cohortdefinition.CohortExpression;
import org.ohdsi.webapi.source.SourceEntity;

public class CohortGenerationRequestBuilder {
    private CohortExpression expression;
    private SourceEntity source;
    private final String sessionId;
    private final String targetSchema;
    private Integer targetId;

    public CohortGenerationRequestBuilder(String sessionId, String targetSchema) {
        this.sessionId = sessionId;
        this.targetSchema = targetSchema;
    }

    public CohortGenerationRequestBuilder withSource(SourceEntity source) {
        this.source = source;
        return this;
    }

    public CohortGenerationRequestBuilder withExpression(CohortExpression expression) {
        this.expression = expression;
        return this;
    }

    public CohortGenerationRequestBuilder withTargetId(Integer targetId) {
        this.targetId = targetId;
        return this;
    }

    public CohortGenerationRequest build() {
        if (this.source == null || this.expression == null || this.targetId == null) {
            throw new RuntimeException(
                    "CohortGenerationRequest should contain non-null expression, source and targetId");
        }

        return new CohortGenerationRequest(expression, source, sessionId, targetId, targetSchema);
    }
}
