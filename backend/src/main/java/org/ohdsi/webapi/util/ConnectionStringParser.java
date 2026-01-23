package org.ohdsi.webapi.util;

import java.util.Arrays;
import java.util.Objects;

import org.ohdsi.webapi.model.DBMSType;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.util.connectionstrings.ConnectionParams;
import org.ohdsi.webapi.util.connectionstrings.ConnectionParamsParser;

public final class ConnectionStringParser {

    public static ConnectionParams getConnectionParams(SourceEntity source) {
        Objects.requireNonNull(source, "Source should not be null");

        var connectionParams = ConnectionParamsParser.parse(getDbmsType(source), source.getSourceConnection());
        if (Objects.isNull(connectionParams.getUser())) {
            connectionParams.setUser(source.getUsername());
        }
        if (Objects.isNull(connectionParams.getPassword())) {
            connectionParams.setPassword(source.getPassword());
        }
        return connectionParams;
    }

    private static DBMSType getDbmsType(SourceEntity source) {
        return Arrays.stream(DBMSType.values())
                .filter(type -> Objects.equals(type.getOhdsiDB(), source.getSourceDialect()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        String.format("Unsupported data source dialect: %s", source.getSourceDialect())));
    }
}
