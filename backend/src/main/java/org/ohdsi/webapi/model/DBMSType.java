package org.ohdsi.webapi.model;

public enum DBMSType {
    POSTGRESQL("PostgreSQL", "postgresql"),
    MS_SQL_SERVER("MS SQL Server", "sql server"),
    PDW("SQL Server Parallel Data Warehouse", "pdw"),
    REDSHIFT("Redshift", "redshift"),
    ORACLE("Oracle", "oracle"),
    IMPALA("Impala", "impala"),
    BIGQUERY("Google BigQuery", "bigquery"),
    NETEZZA("Netezza", "netezza"),
    HIVE("Apache Hive", "hive"),
    SPARK("Spark", "spark"),
    SNOWFLAKE("Snowflake", "snowflake"),
    SYNAPSE("Azure Synapse", "synapse");

    private String label;
    private String ohdsiDB;

    private DBMSType(String label, String ohdsiDB) {
        this.label = label;
        this.ohdsiDB = ohdsiDB;
    }

    public String getValue() {
        return this.toString();
    }

    public String getLabel() {
        return this.label;
    }

    public String getOhdsiDB() {
        return this.ohdsiDB;
    }

    public void setOhdsiDB(String ohdsiDB) {
        this.ohdsiDB = ohdsiDB;
    }
}
