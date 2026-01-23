package org.ohdsi.webapi.vocabulary.cscompare;

import org.ohdsi.circe.vocabulary.ConceptSetExpression;

public class CompareArbitraryDto {
    public ExpressionType[] types;
    public ConceptSetExpression[] compareTargets;
}
