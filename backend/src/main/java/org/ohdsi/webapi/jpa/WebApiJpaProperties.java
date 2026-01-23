package org.ohdsi.webapi.jpa;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

// Note: this class has a WebApi prefix to disambiguate it from the Spring class JpaProperties.
//       Since we're doing custom JPA configuration we need a custom flavor of the JPA properties class.
@ConfigurationProperties(prefix="spring.jpa")
public class WebApiJpaProperties {
    @Valid
    @NestedConfigurationProperty
    private HibernateProperties hibernate = new HibernateProperties();

    @NotNull
    private Boolean showSql;

    public HibernateProperties getHibernate() {
        return hibernate;
    }
    public HibernateProperties hibernate() {
        return hibernate;
    }
    public void setHibernate(HibernateProperties hibernate) {
        this.hibernate = hibernate;
    }

    public boolean getShowSql() {
        return showSql;
    }
    public boolean showSql() {
        return showSql;
    }
    public void setShowSql(boolean showSql) {
        this.showSql = showSql;
    }
}
