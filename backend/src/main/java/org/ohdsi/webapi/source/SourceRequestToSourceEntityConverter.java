package org.ohdsi.webapi.source;

public class SourceRequestToSourceEntityConverter {
    public static SourceEntity convert(SourceRequest request) {

        SourceEntity source = new SourceEntity();
        source.setSourceName(request.getName());
        source.setSourceConnection(request.getConnectionString());
        source.setUsername(request.getUsername());
        source.setPassword(request.getPassword());
        source.setSourceDialect(request.getDialect());
        source.setSourceKey(request.getKey());
        source.setDaimons(request.getDaimons());
        source.getDaimons().forEach(d -> d.setSource(source));
        source.setKeyfileName(request.getKeyfileName());
        if (request.isCheckConnection() != null) {
            source.setCheckConnection(request.isCheckConnection());
        }
        source.setKrbAdminServer(request.getKrbAdminServer());
        source.setKrbAuthMethod(request.getKrbAuthMethod());
        return source;
    }

}
