package org.ohdsi.webapi.generationcache;

import jakarta.persistence.*;
import org.ohdsi.webapi.source.SourceEntity;

import java.util.Date;

@Entity
@Table(name = "generation_cache")
public class GenerationCache {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "generation_cache_generator")
    @SequenceGenerator(name = "generation_cache_generator", sequenceName = "generation_cache_sequence", initialValue = 1, allocationSize = 1)
    private Integer id;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private CacheableGenerationType type;

    @Column(name = "design_hash")
    private Integer designHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private SourceEntity source;

    @Column(name = "result_checksum")
    private String resultChecksum;

    @Column(name = "created_date")
    private Date createdDate = new Date();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public CacheableGenerationType getType() {
        return type;
    }

    public void setType(CacheableGenerationType type) {
        this.type = type;
    }

    public Integer getDesignHash() {
        return designHash;
    }

    public void setDesignHash(Integer designHash) {
        this.designHash = designHash;
    }

    public SourceEntity getSource() {
        return source;
    }

    public void setSource(SourceEntity source) {
        this.source = source;
    }

    public String getResultChecksum() {
        return resultChecksum;
    }

    public void setResultChecksum(String resultChecksum) {
        this.resultChecksum = resultChecksum;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
