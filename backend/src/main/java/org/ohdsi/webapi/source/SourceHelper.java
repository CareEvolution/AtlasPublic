package org.ohdsi.webapi.source;

import org.ohdsi.webapi.authorization.event.ChangeDataSourceEvent;
import org.ohdsi.webapi.model.DBMSType;
import org.ohdsi.webapi.util.ConnectionStringParser;
import org.apache.commons.io.IOUtils;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SourceHelper {
    private final ConcurrentHashMap<SourceEntity, String> sourcesConnections = new ConcurrentHashMap<>();

    public DriverManagerDataSource getDriverManagerDataSource(SourceEntity source) {
        var connectionParams = ConnectionStringParser.getConnectionParams(source);
        DriverManagerDataSource dataSource;
        var connectionString = getSourceConnectionString(source);
        if (connectionParams.getUser() != null && connectionParams.getPassword() != null) {
            // NOTE: jdbc link should NOT include username and password, because they have
            // higher priority than separate ones
            dataSource = new DriverManagerDataSource(
                    connectionString,
                    connectionParams.getUser(),
                    connectionParams.getPassword());
        } else {
            dataSource = new DriverManagerDataSource(connectionString);
        }
        if (DBMSType.SNOWFLAKE.getValue().equalsIgnoreCase(source.getSourceDialect())) {
            if (dataSource.getConnectionProperties() == null) {
                dataSource.setConnectionProperties(new Properties());
            }
            dataSource.getConnectionProperties().setProperty("CLIENT_RESULT_COLUMN_CASE_INSENSITIVE", "true");
        }
        return dataSource;
    }

    private String getSourceConnectionString(SourceEntity source) {
        return sourcesConnections.computeIfAbsent(source, s -> {
            if (Objects.equals(DBMSType.BIGQUERY.getOhdsiDB(), s.getSourceDialect())) {
                return getBQConnectionString(s);
            } else {
                return s.getSourceConnection();
            }
        });
    }

    private String getBQConnectionString(SourceEntity source) {
        var connectionString = source.getSourceConnection();
        if (getBigQueryKeyPath(connectionString) == null) {
            byte[] keyFileData = source.getKeyfile();
            if (Objects.nonNull(keyFileData)) {
                try {
                    File keyFile = java.nio.file.Files.createTempFile("", ".json").toFile();
                    try (OutputStream out = new FileOutputStream(keyFile)) {
                        IOUtils.write(keyFileData, out);
                    }
                    var filePath = keyFile.getAbsolutePath();
                    connectionString = replaceBigQueryKeyPath(connectionString, filePath);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        }

        return connectionString;
    }

    private static String replaceBigQueryKeyPath(String connectionString, String replacement) {
        String cs = Objects.isNull(getBigQueryKeyPath(connectionString))
                ? connectionString + ";OAuthPvtKeyPath=keypath;"
                : connectionString;
        return cs.replaceFirst("(OAuthPvtKeyPath=)(.+?)[;$]", "$1" + Matcher.quoteReplacement(replacement) + ";");
    }

    private static String getBigQueryKeyPath(String connectionString) {
        Matcher matcher = Pattern.compile(".*(OAuthPvtKeyPath=)(.+?)[;$].*").matcher(connectionString);
        return matcher.matches() && matcher.groupCount() > 1 ? matcher.group(2) : null;
    }

    @EventListener
    public void onSourceUpdate(ChangeDataSourceEvent changeDataSourceEvent) {
        var s = new SourceEntity();
        s.setSourceId(changeDataSourceEvent.getId());
        sourcesConnections.remove(s);
    }
}
