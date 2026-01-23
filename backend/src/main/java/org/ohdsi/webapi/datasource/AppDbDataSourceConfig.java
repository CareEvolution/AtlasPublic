package org.ohdsi.webapi.datasource;

import org.ohdsi.webapi.jpa.WebApiJpaProperties;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
public class AppDbDataSourceConfig {
    private final Logger logger = LoggerFactory.getLogger(AppDbDataSourceConfig.class);
    
    private final WebApiJpaProperties jpaProperties;
    private final WebApiProperties webApiProperties;

    public AppDbDataSourceConfig(
            WebApiJpaProperties webApiJpaProperties,
            WebApiProperties webApiProperties) {
        this.jpaProperties = webApiJpaProperties;
        this.webApiProperties = webApiProperties;
    }

    private Properties getJpaHibernateProperties() {
       var hibernate = jpaProperties.hibernate();
       var properties = new Properties();

       // Explicitly set hibernate properties
       properties.setProperty("hibernate.default_schema", hibernate.defaultSchema());
       properties.setProperty("hibernate.generate_statistics",
               hibernate.generateStatistics() ? "true" : "false");
       properties.setProperty("hibernate.jdbc.batch_size", hibernate.jdbc().batchSize().toString());
       properties.setProperty("hibernate.order_inserts",
               hibernate.orderInserts() ? "true" : "false");

       // Set hibernate's show-sql based on the JPA setting
       properties.setProperty("hibernate.show_sql", jpaProperties.showSql() ? "true" : "false");

       //properties.setProperty("hibernate.id.new_generator_mappings", "true");
       return properties;
    }

    private void rearrangeSupportedDrivers() throws Exception {
        String[] supportedDrivers;
        supportedDrivers = new String[] { 
            SupportedDrivers.POSTGRES_DRIVER,
            //SupportedDrivers.SQL_SERVER_DRIVER,
            //SupportedDrivers.ORACLE_DRIVER,
            SupportedDrivers.REDSHIFT_DRIVER,
            //SupportedDrivers.IMPALA_DRIVER,
            //SupportedDrivers.BIGQUERY_DRIVER, 
            //SupportedDrivers.NETEZZA_DRIVER,
            //SupportedDrivers.BIGQUERY_DRIVER_2,
            //SupportedDrivers.HIVE_DRIVER, 
            //SupportedDrivers.SPARK_DRIVER,
            //SupportedDrivers.SNOWFLAKE_DRIVER,
            //SupportedDrivers.DATABRICKS_DRIVER
        };

        // Report out which database drivers have been loaded
        for (String driverName : supportedDrivers) {
            try {
                Class.forName(driverName);
                logger.info("driver loaded: {}", driverName);
            } catch (Exception ex) {
                logger.info("error loading {} driver. {}", driverName, ex.getMessage());
            }
        }

        /*
         * Redshift is based on PostgreSQL, and historically, the PostgreSQL JDBC driver was often used 
         * to connect to Redshift. While Amazon now provides a dedicated Redshift JDBC driver and recommends 
         * its use, both drivers may register themselves to handle JDBC URLs that begin with jdbc:postgresql://.
         * To prevent the Redshift driver from being selected when we need the PostgreSQL driver,
         * we ensure that the Redshift driver is the last one registered with the DriverManager.
         */
        var drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver d = drivers.nextElement();
            if (d.getClass().getName().contains("com.amazon.redshift.jdbc")) {
                try {
                    DriverManager.deregisterDriver(d);
                    DriverManager.registerDriver(d);
                } catch (SQLException e) {
                    throw new RuntimeException("Could not move redshift driver to the end of the drivers list", e);
                }
            }
        }
    }


    @Bean
    @Primary
    public DataSource appDbDataSource() {
        // TODO need to investigate pooling
        // Note: the "normal" datasource builder could not set the schema name, which is
        //       needed by Spring Batch, which uses JDBC to access the BATCH_ tables.  So
        //       reverting to using a DriverManagerDataSource, which allows the schema
        //       name to be set.

        try {
            // This needs to be done prior to creating the datasource to ensure that
            // the correct driver is used for all connections to the database.
            rearrangeSupportedDrivers();
        } catch (Exception e) {
            throw new RuntimeException("Could not set up primary webapi database", e);
        }

        var ds = new DriverManagerDataSource();
        ds.setDriverClassName(SupportedDrivers.POSTGRES_DRIVER);
        ds.setUrl(webApiProperties.appDb().url());
        ds.setSchema(webApiProperties.appDb().schemaName());
        ds.setUsername(webApiProperties.appDb().username());
        ds.setPassword(webApiProperties.appDb().password());
        return ds;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("appDbDataSource") DataSource dataSource) {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        vendorAdapter.setShowSql(false);

        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setJpaProperties(getJpaHibernateProperties());
        factory.setPackagesToScan("org.ohdsi.webapi");
        factory.setDataSource(dataSource);
        return factory;
    }

    @Bean
    @Primary
    // This is needed so that JpaTransactionManager is used for autowiring, instead of
    // DataSourceTransactionManager
    public PlatformTransactionManager transactionManager(
           LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
    }
}
