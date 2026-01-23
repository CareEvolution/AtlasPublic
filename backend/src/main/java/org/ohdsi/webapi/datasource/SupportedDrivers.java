package org.ohdsi.webapi.datasource;

public class SupportedDrivers {
    public static final String POSTGRES_DRIVER = "org.postgresql.Driver";
    public static final String REDSHIFT_DRIVER = "com.amazon.redshift.jdbc.Driver";
    public static final String SQL_SERVER_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    public static final String ORACLE_DRIVER = "oracle.jdbc.driver.OracleDriver";
    public static final String IMPALA_DRIVER = "com.cloudera.impala.jdbc.Driver";
    public static final String BIGQUERY_DRIVER = "net.starschema.clouddb.jdbc.BQDriver";
    public static final String BIGQUERY_DRIVER_2 = "com.simba.googlebigquery.jdbc42.Driver";
    public static final String SNOWFLAKE_DRIVER = "net.snowflake.client.jdbc.SnowflakeDriver";
    public static final String HIVE_DRIVER = "org.apache.hive.jdbc.HiveDriver";
    public static final String SPARK_DRIVER = "com.simba.spark.jdbc.Driver";
    public static final String DATABRICKS_DRIVER = "com.databricks.client.jdbc.Driver";
    public static final String NETEZZA_DRIVER = "org.netezza.Driver";
}
