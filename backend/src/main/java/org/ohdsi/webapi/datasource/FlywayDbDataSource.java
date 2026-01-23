package org.ohdsi.webapi.datasource;

import javax.sql.DataSource;

import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class FlywayDbDataSource {
    private final Logger logger = LoggerFactory.getLogger(FlywayDbDataSource.class);
    private final WebApiProperties webApiProperties;

    public FlywayDbDataSource(WebApiProperties webApiProperties) {
        this.webApiProperties = webApiProperties;
    }

    @Bean(defaultCandidate = false)
    @FlywayDataSource
    public DataSource flywayDataSource() {

        // NOTE: Flyway requires its user to have CREATE/ALTER/DROP/etc. privileges
        // If you don't want the default DataSource to have those privileges then you
        // have the opportunity to use a different username/password here.

        logger.info("Flyway datasource URL is: " + webApiProperties.security().authDb().url());

        var ds = new DriverManagerDataSource();
        ds.setDriverClassName(SupportedDrivers.POSTGRES_DRIVER);
        ds.setUrl(webApiProperties.appDb().url());
        ds.setSchema(webApiProperties.appDb().schemaName());
        ds.setUsername(webApiProperties.appDb().flywayUsername());
        ds.setPassword(webApiProperties.appDb().flywayPassword());
        return ds;
    }
}
