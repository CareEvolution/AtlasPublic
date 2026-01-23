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

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;

import java.io.Serializable;
import java.util.Objects;


@Entity(name = "SourceDaimon")
@Table(name = "source_daimon")
@SQLDelete(sql = "UPDATE {h-schema}source_daimon SET priority = -1 WHERE SOURCE_DAIMON_ID = ?")
// @Where(clause = "priority >= 0")
public class SourceDaimonEntity implements Serializable {
    public enum DaimonType {
        CDM, Vocabulary, Results, CEM, CEMResults, Temp
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "source_daimon_id_generator")
    @SequenceGenerator(name = "source_daimon_id_generator", sequenceName = "source_daimon_sequence", initialValue = 1, allocationSize = 1)
    @Column(name = "source_daimon_id")
    private int sourceDaimonId;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "source_id", referencedColumnName = "source_id")
    private SourceEntity source;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "daimon_type")
    private DaimonType daimonType;

    @Column(name = "table_qualifier")
    private String tableQualifier;

    @Column(name = "priority")
    private Integer priority;

    public int getSourceDaimonId() {
        return sourceDaimonId;
    }

    public void setSourceDaimonId(int sourceDaimonId) {
        this.sourceDaimonId = sourceDaimonId;
    }

    public SourceEntity getSource() {
        return source;
    }

    public void setSource(SourceEntity source) {
        this.source = source;
    }

    public DaimonType getDaimonType() {
        return daimonType;
    }

    public void setDaimonType(DaimonType daimonType) {
        this.daimonType = daimonType;
    }

    public String getTableQualifier() {
        return tableQualifier;
    }

    public void setTableQualifier(String tableQualifier) {
        this.tableQualifier = tableQualifier;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SourceDaimonEntity))
            return false;
        SourceDaimonEntity that = (SourceDaimonEntity) o;
        return Objects.equals(getSource(), that.getSource()) &&
                Objects.equals(getDaimonType(), that.getDaimonType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSource(), getDaimonType());
    }

    @Override
    public String toString() {
        return String.format("sourceDaimonId = %d, daimonType = %s, tableQualifier = %s, priority = %d", sourceDaimonId,
                daimonType, tableQualifier, priority);
    }
}
