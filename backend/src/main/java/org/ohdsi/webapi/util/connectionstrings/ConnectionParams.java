package org.ohdsi.webapi.util.connectionstrings;

public class ConnectionParams {
    private String dbms;
    private String user;
    private String password;
    private String connectionString;

    public String getDbms() {

        return dbms;
    }

    public void setDbms(String dbms) {

        this.dbms = dbms;
    }
    
    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getUser() {

        return user;
    }

    public void setUser(String user) {

        this.user = user;
    }

    public String getPassword() {

        return password;
    }

    public void setPassword(String password) {

        this.password = password;
    }

}
