package org.ohdsi.webapi.achilles.domain;

import org.ohdsi.webapi.source.SourceEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "achilles_cache")
public class AchillesCacheEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "achilles_cache_generator")
    @SequenceGenerator(name = "achilles_cache_generator", sequenceName = "achilles_cache_seq", initialValue = 1, allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private SourceEntity source;

    @Column(name = "cache_name", nullable = false)
    private String cacheName;

    @Column(name = "cache")
    private String cache;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SourceEntity getSource() {
        return source;
    }

    public void setSource(SourceEntity source) {
        this.source = source;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getCache() {
        return cache;
    }

    public void setCache(String cacheName) {
        this.cache = cacheName;
    }
}
