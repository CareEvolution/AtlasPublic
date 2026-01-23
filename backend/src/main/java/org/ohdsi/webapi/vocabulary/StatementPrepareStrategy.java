package org.ohdsi.webapi.vocabulary;

import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.util.PreparedStatementRenderer;

import java.util.function.Function;

public interface StatementPrepareStrategy {
    PreparedStatementRenderer prepareStatement(SourceEntity source, Function<String, String> queryModifier);
}