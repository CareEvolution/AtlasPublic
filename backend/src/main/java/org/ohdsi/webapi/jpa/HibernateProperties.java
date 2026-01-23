package org.ohdsi.webapi.jpa;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class HibernateProperties {
    @NotBlank
    private String defaultSchema;

    @NotNull
    private Boolean generateStatistics;

    @Valid
    @NestedConfigurationProperty
    private JdbcProperties jdbc = new JdbcProperties();

    @NotNull
    private Boolean orderInserts;

    public String getDefaultSchema() {
        return defaultSchema;
    }
    public String defaultSchema() {
        return defaultSchema;
    }
    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    public Boolean getGenerateStatistics() {
        return generateStatistics;
    }
    public Boolean generateStatistics() {
        return generateStatistics;
    }
    public void setGenerateStatistics(Boolean generateStatistics) {
        this.generateStatistics = generateStatistics;
    }

    public JdbcProperties getJdbc() {
        return jdbc;
    }
    public JdbcProperties jdbc() {
        return jdbc;
    }
    public void setJdbc(JdbcProperties jdbc) {
        this.jdbc = jdbc;
    }

    public Boolean getOrderInserts() {
        return orderInserts;
    }
    public Boolean orderInserts() {
        return orderInserts;
    }
    public void setOrderInserts(Boolean orderInserts) {
        this.orderInserts = orderInserts;
    }
}
