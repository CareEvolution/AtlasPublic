package org.ohdsi.webapi.common;

import org.ohdsi.webapi.source.SourceEntity;

import java.util.function.Function;

public final class SourceMapKey<T> {
    public static SourceMapKey<String> BY_SOURCE_KEY = new SourceMapKey<>(SourceEntity::getSourceKey);
    public static SourceMapKey<Integer> BY_SOURCE_ID = new SourceMapKey<>(SourceEntity::getSourceId);

    private Function<SourceEntity, T> keyFunc;

    private SourceMapKey(Function<SourceEntity, T> keyFunc) {
        this.keyFunc = keyFunc;
    }

    public Function<SourceEntity, T> getKeyFunc() {
        return keyFunc;
    }
}
