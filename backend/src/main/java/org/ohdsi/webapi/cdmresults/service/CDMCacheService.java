package org.ohdsi.webapi.cdmresults.service;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.ohdsi.webapi.cdmresults.converter.DescendantRecordAndPersonCountToCDMCacheEntity;
import org.ohdsi.webapi.cdmresults.domain.CDMCacheEntity;
import org.ohdsi.webapi.cdmresults.mapper.DescendantRecordAndPersonCountMapper;
import org.ohdsi.webapi.cdmresults.repository.CDMCacheRepository;
import org.ohdsi.webapi.source.SourceDaimonEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.transactiontemplates.TransactionTemplates;
import org.ohdsi.webapi.util.PreparedSqlRender;
import org.ohdsi.webapi.util.PreparedStatementRenderer;
import org.ohdsi.webapi.util.SessionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CDMCacheService  {
    private static final Logger log = LoggerFactory.getLogger(CDMCacheService.class);

    private static final String CONCEPT_SQL = "/cdmresults/sql/getConceptsForBatch.sql";
    private static final String PARTIAL_CONCEPT_COUNT_PERSON_SQL = "cdmresults/sql/getConceptRecordPersonCount.sql";
    private static final String BATCH_CONCEPT_COUNT_PERSON_SQL = "cdmresults/sql/getBatchConceptRecordPersonCount.sql";

    private final CDMCacheBatchService cdmCacheBatchService;
    private final CDMCacheRepository cdmCacheRepository;
    private final SourceService sourceService;
    private final TransactionTemplates transactionTemplates;

    public CDMCacheService(
            CDMCacheBatchService cdmCacheBatchService,
            CDMCacheRepository cdmCacheRepository,
            SourceService sourceService,
            TransactionTemplates transactionTemplates) {
        this.cdmCacheBatchService = cdmCacheBatchService;
        this.cdmCacheRepository = cdmCacheRepository;
        this.sourceService = sourceService;
        this.transactionTemplates = transactionTemplates;
    }

    public void warm(SourceEntity source) {
        try {
            var transactionTemplate = transactionTemplates.getTransactionTemplateRequiresNew();

            transactionTemplate.execute(s -> {
                // Create full cache
                cacheRecordsBatch(source);
                return null;
            });
        } catch (Exception ex) {
            log.error("Failed to warm cache {}. Exception: {}", source.getSourceKey(), ex.getLocalizedMessage());
        }
    }

    public List<CDMCacheEntity> findAndCache(SourceEntity source, List<Integer> conceptIds) {
        if (CollectionUtils.isEmpty(conceptIds)) {
            return Collections.emptyList();
        }

        var cacheEntities = find(source, conceptIds);
        // Get list of concept identifiers
        var cacheConceptIds = cacheEntities.stream()
                .map(CDMCacheEntity::getConceptId)
                .collect(Collectors.toList());
        // If number of cached entities is not equal to the number of requested entities then
        // we have to get data from source
        if (cacheEntities.size() != conceptIds.size()) {
            var cacheAbsentConceptIds = new ArrayList<>(
                    CollectionUtils.subtract(conceptIds, cacheConceptIds));
            cacheRecordsById(source, cacheAbsentConceptIds);
            var cacheAbsentEntities = find(source, cacheAbsentConceptIds);
            cacheEntities.addAll(cacheAbsentEntities);
        }

        return cacheEntities;
    }

    @Transactional()
    public void clearCache() {
        var sources = sourceService.findAll();
        sources.stream().forEach(this::clearCache);
    }

    @Transactional()
    public void clearCache(SourceEntity source) {
        cdmCacheRepository.deleteBySource(source.getSourceId());
    }

    private List<CDMCacheEntity> find(SourceEntity source, List<Integer> conceptIds) {
        if (CollectionUtils.isEmpty(conceptIds)) {
            return Collections.emptyList();
        }

        int start = 0, size = conceptIds.size();
        var cacheEntities = new ArrayList<CDMCacheEntity>();
        int parameterLimit = PreparedSqlRender.getParameterLimit(source);
        // Get cached entities by calling the query for small pieces of
        // the array of concept identifiers
        while (start < size) {
            int end = Math.min(start + parameterLimit, size);
            var idsSlice = conceptIds.subList(start, end);
            start += parameterLimit;
            cacheEntities.addAll(cdmCacheRepository.findBySourceAndConceptIds(source.getSourceId(), idsSlice));
        }
        return cacheEntities;
    }

    private void cacheRecordsBatch(SourceEntity source) {
        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        // Full cache
        // Make sure that query returns ordered collection of ids or sort it after query is executed
        var cpsr = getConceptPreparedStatementRenderer(source);
        var minMaxPairs = jdbcTemplate.query(cpsr.getSql(), cpsr.getSetter(), (rs, rowNum) -> {
            return new ImmutablePair<>(rs.getInt("min_concept"), rs.getInt("max_concept"));
        });
        minMaxPairs.forEach(pair -> {
            var psr = getBatchPreparedStatementRenderer(source, pair.getLeft(), pair.getRight());
            loadCache(source, psr, jdbcTemplate);
        });
    }

    private void cacheRecordsById(SourceEntity source, List<Integer> ids) {
        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);

        // In case of getting records for concrete concepts we must call the query for small pieces of
        // the array of concept identifiers
        // places in the target query so the parameter limit will need to be divided
        int parameterLimit = PreparedSqlRender.getParameterLimit(source);
        int start = 0, size = ids.size();
        while (start < size) {
            int end = Math.min(start + parameterLimit, size);
            var idsSlice = ids.subList(start, end);
            start += parameterLimit;
            var psr = getPartialPreparedStatementRenderer(source, idsSlice);
            var cachedIds = loadCache(source, psr, jdbcTemplate);
            // in this batch, need to save any concepts that were not found when loading cache
            var notFoundIds = new ArrayList<>(CollectionUtils.subtract(idsSlice, cachedIds));
            if (!notFoundIds.isEmpty()) { // store zeros in cache
                var zeroConcepts = notFoundIds.stream().map(id -> {
                    var ce = new CDMCacheEntity();
                    ce.setConceptId(id);
                    ce.setRecordCount(0L);
                    ce.setDescendantRecordCount(0L);
                    ce.setPersonCount(0L);
                    ce.setDescendantPersonCount(0L);
                    return ce;
                }).collect(Collectors.toList());
                cdmCacheBatchService.save(source, zeroConcepts); // zero concepts will be less than batch size
            }
        }
    }

    private Set<Integer> loadCache(SourceEntity source,
            PreparedStatementRenderer psr,
            JdbcTemplate jdbcTemplate) {
        int BATCH_SIZE = 2000;
        var rows = new ArrayList<CDMCacheEntity>(BATCH_SIZE);
        Set<Integer> cachedIds = new HashSet<>();
        jdbcTemplate.setFetchSize(BATCH_SIZE);
        jdbcTemplate.query(psr.getSql(), psr.getSetter(), resultSet -> {
            var row = DescendantRecordAndPersonCountMapper.mapRow(resultSet);
            var cacheEntity = DescendantRecordAndPersonCountToCDMCacheEntity.convert(row);
            cachedIds.add(cacheEntity.getConceptId());
            rows.add(cacheEntity);
            if (rows.size() == BATCH_SIZE) {
                // Persist or merge batch
                cdmCacheBatchService.save(source, rows);
                rows.clear();
            }
        });
        if (!rows.isEmpty()) {
            cdmCacheBatchService.save(source, rows);
        }
        return cachedIds;
    }

    private PreparedStatementRenderer getConceptPreparedStatementRenderer(SourceEntity source) {
        var resultTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);

        String[] tables = { "resultTableQualifier" };
        String[] tableValues = { resultTableQualifier };
        String[] params = { "batch_size" };
        Object[] values = { 100000 };

        return new PreparedStatementRenderer(source, CONCEPT_SQL, tables, tableValues, params, values,
                SessionUtils.sessionId());
    }

    private PreparedStatementRenderer getPartialPreparedStatementRenderer(SourceEntity source, List<Integer> ids) {
        var resultTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var vocabularyTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);

        String[] tables = { "resultTableQualifier", "vocabularyTableQualifier" };
        String[] tableValues = { resultTableQualifier, vocabularyTableQualifier };
        // Caching of concrete concepts
        Integer[] identifiers = ids.toArray(new Integer[0]);

        return new PreparedStatementRenderer(source, 
                PARTIAL_CONCEPT_COUNT_PERSON_SQL, tables, tableValues,
                "conceptIdentifiers", identifiers);
    }

    private PreparedStatementRenderer getBatchPreparedStatementRenderer(SourceEntity source, int min, int max) {
        var resultTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        var vocabularyTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);

        String[] tables = { "resultTableQualifier", "vocabularyTableQualifier" };
        String[] tableValues = { resultTableQualifier, vocabularyTableQualifier };
        // Caching of concrete concepts
        String[] variables = new String[] { "conceptIdentifierMin", "conceptIdentifierMax" };
        Integer[] identifiers = new Integer[] { min, max };

        return new PreparedStatementRenderer(source, BATCH_CONCEPT_COUNT_PERSON_SQL, tables, tableValues,
                variables, identifiers);
    }
}
