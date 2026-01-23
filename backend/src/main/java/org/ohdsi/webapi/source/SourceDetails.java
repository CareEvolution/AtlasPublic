package org.ohdsi.webapi.source;

public class SourceDetails extends SourceInfo {
    private String connectionString;
    private String username;
    private String password;
    private String krbAuthMethod;
    private String keyfileName;
    private String krbAdminServer;

    public SourceDetails(SourceEntity s, boolean maskSensitiveInfo) {
        super(s);
        setConnectionString(maskSensitiveInfo ? SourceEntity.MASKED_CONNECTION_STRING : s.getSourceConnection());
        setKeyfileName(s.getKeyfileName());
        setUsername(maskSensitiveInfo ? SourceEntity.MASKED_USERNAME : s.getUsername());
        setPassword(maskSensitiveInfo ? SourceEntity.MASKED_PASSWORD : s.getPassword());
        setKrbAuthMethod(s.getKrbAuthMethod());
        setKrbAdminServer(s.getKrbAdminServer());
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getKeyfileName() {
        return keyfileName;
    }

    public void setKeyfileName(String keyfileName) {
        this.keyfileName = keyfileName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getKrbAuthMethod() {
        return krbAuthMethod;
    }

    public void setKrbAuthMethod(String krbAuthMethod) {
        this.krbAuthMethod = krbAuthMethod;
    }

    public String getKrbAdminServer() {
        return krbAdminServer;
    }

    public void setKrbAdminServer(String krbAdminServer) {
        this.krbAdminServer = krbAdminServer;
    }
}
