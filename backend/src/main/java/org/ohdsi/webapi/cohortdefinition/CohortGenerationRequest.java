package org.ohdsi.webapi.cohortdefinition;

import org.ohdsi.circe.cohortdefinition.CohortExpression;
import org.ohdsi.webapi.source.SourceEntity;

public class CohortGenerationRequest {
    private final CohortExpression expression;
    private final SourceEntity source;
    private final String sessionId;
    private final String targetSchema;
    private final Integer targetId;

    public CohortGenerationRequest(CohortExpression expression, SourceEntity source, String sessionId, Integer targetId,
            String targetSchema) {
        this.expression = expression;
        this.source = source;
        this.sessionId = sessionId;
        this.targetId = targetId;
        this.targetSchema = targetSchema;
    }

    public CohortExpression getExpression() {
        return expression;
    }

    public SourceEntity getSource() {
        return source;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTargetSchema() {
        return targetSchema;
    }

    public Integer getTargetId() {
        return targetId;
    }
}
