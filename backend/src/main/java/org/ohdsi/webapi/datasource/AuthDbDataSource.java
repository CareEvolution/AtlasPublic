/*
 *
 * Copyright 2017 Observational Health Data Sciences and Informatics
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors: Mikhail Mironov, Vitaly Koulakov
 *
 */
package org.ohdsi.webapi.datasource;

import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration("authDataSourceConfig")
@ConditionalOnProperty(name = "webapi.security.mode", havingValue = "database")
public class AuthDbDataSource {
    private final Logger logger = LoggerFactory.getLogger(AuthDbDataSource.class);
    private final WebApiProperties webApiProperties;

    public AuthDbDataSource(WebApiProperties webApiProperties) {
        this.webApiProperties = webApiProperties;
    }

    @Bean(name = "AuthDbDataSource")
    public DataSource authDataSource() {
        logger.info("AuthDb datasource URL is: " + webApiProperties.security().authDb().url());

        var ds = new DriverManagerDataSource();
        ds.setDriverClassName(SupportedDrivers.POSTGRES_DRIVER);
        ds.setUrl(webApiProperties.security().authDb().url());
        ds.setSchema(webApiProperties.security().authDb().schemaName());
        ds.setUsername(webApiProperties.security().authDb().username());
        ds.setPassword(webApiProperties.security().authDb().password());
        return ds;

    }
}
