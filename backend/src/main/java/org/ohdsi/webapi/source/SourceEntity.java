/*
 * Copyright 2015 fdefalco.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ohdsi.webapi.source;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.ohdsi.webapi.model.SecuredEntity;
import org.ohdsi.webapi.source.SourceDaimonEntity.DaimonType;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;


@Entity(name = "Source")
@Table(name = "source")
@SQLDelete(sql = "UPDATE {h-schema}source SET deleted_date = current_timestamp WHERE SOURCE_ID = ?")
@SQLRestriction("deleted_date IS NULL")
public class SourceEntity extends SecuredEntity<Integer> {
    public static final String MASKED_USERNAME = "<username>";
    public static final String MASKED_PASSWORD = "<password>";
    public static final String MASKED_CONNECTION_STRING = "<connection_string>";

    public static final String IMPALA_DATASOURCE = "impala";
    public static final String BIGQUERY_DATASOURCE = "bigquery";
    public static final List<String> DBMS_KEYTAB_SUPPORT = ImmutableList.of(IMPALA_DATASOURCE, BIGQUERY_DATASOURCE);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "source_id_generator")
    @SequenceGenerator(name = "source_id_generator", sequenceName = "source_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "source_id")
    private int sourceId;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "source")
    @SQLRestriction("priority >= 0")
    private Collection<SourceDaimonEntity> daimons;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "source_dialect")
    private String sourceDialect;

    @Convert(converter = SecretAttributeConverter.class)
    @Column(name = "source_connection")
    private String sourceConnection;

    @Column(name = "source_key")
    private String sourceKey;

    @Convert(converter = SecretAttributeConverter.class)
    @Column(name = "username")
    private String username;

    @Convert(converter = SecretAttributeConverter.class)
    @Column(name = "password")
    private String password;

    @Column(name = "deleted_date")
    private Date deletedDate;

    @Column(name = "krb_keytab")
    private byte[] keyfile;

    @Column(name = "keytab_name")
    private String keyfileName;

    @Column(name = "krb_admin_server")
    private String krbAdminServer;

    @Column(name = "krb_auth_method")
    private String krbAuthMethod;

    @Column(name = "is_cache_enabled")
    private boolean isCacheEnabled;

    @Column(name = "check_connection")
    private boolean checkConnection = true;

    public String getTableQualifier(DaimonType daimonType) {
        String result = getTableQualifierOrNull(daimonType);
        if (result == null)
            throw new RuntimeException("DaimonType (" + daimonType + ") not found in Source");
        return result;
    }

    public String getTableQualifierOrNull(DaimonType daimonType) {
        if (this.getDaimons() != null) {
            for (SourceDaimonEntity sourceDaimon : this.getDaimons()) {
                if (sourceDaimon.getDaimonType() == daimonType) {
                    return sourceDaimon.getTableQualifier();
                }
            }
            if (DaimonType.Vocabulary.equals(daimonType)) {
                return getDaimons().stream()
                        .filter(d -> DaimonType.CDM.equals(d.getDaimonType()))
                        .map(SourceDaimonEntity::getTableQualifier)
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public Collection<SourceDaimonEntity> getDaimons() {
        return daimons;
    }

    public void setDaimons(Collection<SourceDaimonEntity> daimons) {
        this.daimons = daimons;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceDialect() {
        return sourceDialect;
    }

    public void setSourceDialect(String sourceDialect) {
        this.sourceDialect = sourceDialect;
    }

    public String getSourceConnection() {
        return sourceConnection;
    }

    public void setSourceConnection(String sourceConnection) {
        this.sourceConnection = sourceConnection;
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

    public byte[] getKeyfile() {
        return keyfile;
    }

    public void setKeyfile(byte[] keyfile) {
        this.keyfile = keyfile;
    }

    public String getKeyfileName() {
        return keyfileName;
    }

    public void setKeyfileName(String keyfileName) {
        this.keyfileName = keyfileName;
    }

    public String getKrbAdminServer() {
        return krbAdminServer;
    }

    public void setKrbAdminServer(String krbAdminServer) {
        this.krbAdminServer = krbAdminServer;
    }

    public String getKrbAuthMethod() {
        return krbAuthMethod;
    }

    public void setKrbAuthMethod(String krbAuthMethod) {
        this.krbAuthMethod = krbAuthMethod;
    }

    public boolean supportsKeyfile() {
        return false;
    }

    public boolean isIsCacheEnabled() {
        return isCacheEnabled;
    }

    public void setIsCacheEnabled(boolean isCacheEnabled) {
        this.isCacheEnabled = isCacheEnabled;
    }

    public boolean isCheckConnection() {
        return checkConnection;
    }

    public void setCheckConnection(boolean checkConnection) {
        this.checkConnection = checkConnection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SourceEntity source = (SourceEntity) o;
        return sourceId == source.sourceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId);
    }

    @Override
    public String toString() {
        return "sourceId=" + sourceId +
                ", daimons=" + daimons +
                ", sourceName='" + sourceName + '\'' +
                ", sourceDialect='" + sourceDialect + '\'' +
                ", sourceKey='" + sourceKey + '\'' +
                ", isCacheEnabled='" + isCacheEnabled;
    }

    @Override
    public Integer getId() {
        return sourceId;
    }
}
