package org.ohdsi.webapi.vocabulary;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.ohdsi.analysis.Utils;
import org.ohdsi.circe.cohortdefinition.ConceptSet;
import org.ohdsi.circe.vocabulary.ConceptSetExpression;
import org.ohdsi.circe.vocabulary.ConceptSetExpressionQueryBuilder;
import org.ohdsi.sql.SqlRender;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.vocabulary.Concept;
import org.ohdsi.webapi.conceptset.ConceptSetComparison;
import org.ohdsi.webapi.conceptset.ConceptSetExport;
import org.ohdsi.webapi.conceptset.ConceptSetOptimizationResult;
import org.ohdsi.webapi.conceptset.converter.ConceptSetToExportConverter;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.source.SourceDaimonEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceInfo;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.util.PreparedSqlRender;
import org.ohdsi.webapi.util.PreparedStatementRenderer;
import org.ohdsi.webapi.vocabulary.cscompare.CompareArbitraryDto;
import org.ohdsi.webapi.vocabulary.cscompare.ConceptSetCompareService;
import org.ohdsi.webapi.vocabulary.cscompare.ExpressionFileUtils;
import org.ohdsi.webapi.vocabulary.dto.ConceptSearchRequestDTO;
import org.ohdsi.webapi.vocabulary.dto.LookupAncestorsAndDescendantsRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.ohdsi.webapi.util.SecurityUtils.whitelist;
import static org.ohdsi.webapi.vocabulary.cscompare.ConceptSetCompareService.CONCEPT_SET_COMPARISON_ROW_MAPPER;

@Service
public class VocabularyService {
    private static final Logger log = LoggerFactory.getLogger(VocabularyService.class);

    private static Hashtable<String, VocabularyInfo> vocabularyInfoCache = null;
    private static final String VOCAB_SCHEMA = "vocab_schema";

    private final ConceptSetCompareService conceptSetCompareService;
    private final SourceService sourceService;
    private final StringResourceReader resourceReader;

    public VocabularyService(
            ConceptSetCompareService conceptSetCompareService,
            SourceService sourceService,
            StringResourceReader resourceReader) {
        this.conceptSetCompareService = conceptSetCompareService;
        this.sourceService = sourceService;
        this.resourceReader = resourceReader;
    }

    private final RowMapper<Concept> rowMapper = (resultSet, arg1) -> {
        final Concept concept = new Concept();
        concept.conceptId = resultSet.getLong("CONCEPT_ID");
        concept.conceptCode = resultSet.getString("CONCEPT_CODE");
        concept.conceptName = resultSet.getString("CONCEPT_NAME");
        concept.standardConcept = resultSet.getString("STANDARD_CONCEPT");
        concept.invalidReason = resultSet.getString("INVALID_REASON");
        concept.conceptClassId = resultSet.getString("CONCEPT_CLASS_ID");
        concept.vocabularyId = resultSet.getString("VOCABULARY_ID");
        concept.domainId = resultSet.getString("DOMAIN_ID");
        concept.validStartDate = resultSet.getDate("VALID_START_DATE");
        concept.validEndDate = resultSet.getDate("VALID_END_DATE");
        return concept;
    };

    public RowMapper<Concept> getRowMapper() {
        return this.rowMapper;
    }

    public String getDefaultVocabularySourceKey() {
        var vocabSource = sourceService.getPriorityVocabularySource();
        return Objects.nonNull(vocabSource) ? vocabSource.getSourceKey() : null;
    }

    public SourceEntity getPriorityVocabularySource() {
        var source = sourceService.getPriorityVocabularySource();
        if (Objects.isNull(source)) {
            throw new NotFoundException("There is no priority vocabulary source configured");
        }
        return source;
    }

    public ConceptSetExport exportConceptSet(ConceptSet conceptSet, SourceInfo vocabSource) {
        var export = ConceptSetToExportConverter.convert(conceptSet);
        // Lookup the identifiers
        export.identifierConcepts = executeIncludedConceptLookup(vocabSource.sourceKey, conceptSet.expression);
        // Lookup the mapped items
        export.mappedConcepts = executeMappedLookup(vocabSource.sourceKey, conceptSet.expression);
        return export;
    }

    public Map<Long, List<Long>> calculateAscendants(SourceEntity source, LookupAncestorsAndDescendantsRequestDTO ids) {
        if (CollectionUtils.isEmpty(ids.ancestors()) || CollectionUtils.isEmpty(ids.descendants())) {
            return new HashMap<>();
        }

        final int limit = Math.floorDiv(PreparedSqlRender.getParameterLimit(source), 2);

        final var result = new ArrayList<Map.Entry<Long, Long>>();

        // Here we calculate cartesian product of batches
        for (final var ancestorsBatch : Lists.partition(ids.ancestors(), limit)) {
            for (final var descendantsBatch : Lists.partition(ids.descendants(), limit)) {
                final var psr = prepareAscendantsCalculating(
                        ancestorsBatch.toArray(new Long[0]),
                        descendantsBatch.toArray(new Long[0]),
                        source
                );

                result.addAll(sourceService.getSourceJdbcTemplate(source)
                        .query(
                                psr.getSql(),
                                psr.getSetter(),
                                (resultSet, arg1) -> Maps.immutableEntry(resultSet.getLong("ANCESTOR_ID"),
                                        resultSet.getLong("DESCENDANT_ID"))));
            }
        }

        return result
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Map.Entry::getValue,
                                Collectors.mapping(
                                        Map.Entry::getKey,
                                        Collectors.toList()
                                )
                        )
                );
    }

    protected PreparedStatementRenderer prepareAscendantsCalculating(
            Long[] identifiers,
            Long[] descendants,
            SourceEntity source) {
        var sqlPath = "vocabulary/sql/calculateAscendants.sql";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);

        return new PreparedStatementRenderer(source, sqlPath, VOCAB_SCHEMA, tqValue,
                new String[] { "ancestors", "descendants" },
                new Object[] { identifiers, descendants });
    }

    public Collection<Concept> executeIdentifierLookup(SourceEntity source, long[] identifiers) {
        Collection<Concept> concepts = new ArrayList<>();
        if (identifiers.length == 0) {
            return concepts;
        } else {
            // Determine if we need to chunk up ther request based on the parameter
            // limit of the source RDBMS
            int parameterLimit = PreparedSqlRender.getParameterLimit(source);
            if (parameterLimit > 0 && identifiers.length > parameterLimit) {
                concepts = executeIdentifierLookup(source,
                        Arrays.copyOfRange(identifiers, parameterLimit, identifiers.length));
                identifiers = Arrays.copyOfRange(identifiers, 0, parameterLimit);
            }

            var psr = prepareExecuteIdentifierLookup(identifiers, source);
            var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
            return concepts.addAll(jdbcTemplate.query(psr.getSql(), psr.getSetter(), this.rowMapper))
                    ? concepts : new ArrayList<>();
        }
    }

    protected PreparedStatementRenderer prepareExecuteIdentifierLookup(
            long[] identifiers,
            SourceEntity source) {
        var sqlPath = "vocabulary/sql/lookupIdentifiers.sql";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);

        return new PreparedStatementRenderer(
                source,
                sqlPath,
                VOCAB_SCHEMA,
                tqValue,
                "identifiers", identifiers);
    }

    public Collection<Concept> executeIncludedConceptLookup(String sourceKey,
            ConceptSetExpression conceptSetExpression) {
        var source = sourceService.findBySourceKey(sourceKey);
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var builder = new ConceptSetExpressionQueryBuilder();
        var query = builder.buildExpressionQuery(conceptSetExpression);
        var psr = new PreparedStatementRenderer(source, query, "vocabulary_database_schema",
                tqValue);
        var sqlPath = "vocabulary/sql/lookupIdentifiers.sql";
        var searches = new String[] { "identifiers", VOCAB_SCHEMA };
        var replacements = new String[] { psr.getSql(), tqValue };
        psr = new PreparedStatementRenderer(source, sqlPath, searches, replacements, (String[]) null, null);
        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(), this.rowMapper);
    }

    public Collection<Concept> executeSourcecodeLookup(SourceEntity source, String[] sourcecodes) {
        if (sourcecodes.length == 0) {
            return new ArrayList<>();
        }
        var psr = prepareExecuteSourcecodeLookup(sourcecodes, source);
        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(), this.rowMapper);
    }

    protected PreparedStatementRenderer prepareExecuteSourcecodeLookup(String[] sourcecodes, SourceEntity source) {
        var sqlPath = "vocabulary/sql/lookupSourcecodes.sql";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var psr = new PreparedStatementRenderer(
                source,
                sqlPath,
                VOCAB_SCHEMA,
                tqValue,
                "sourcecodes",
                sourcecodes);
        return psr;
    }

    protected Collection<Concept> executeMappedLookup(SourceEntity source, long[] identifiers) {
        Collection<Concept> concepts = new HashSet<>();
        if (identifiers.length == 0) {
            return concepts;
        } else {
            // Determine if we need to chunk up the request based on the parameter
            // limit of the source RDBMS
            int parameterLimit = PreparedSqlRender.getParameterLimit(source);
            // Next take into account the fact that the identifiers are used in 3
            // places in the query so the parameter limit will need to be divided
            parameterLimit = Math.floorDiv(parameterLimit, 3);
            if (parameterLimit > 0 && identifiers.length > parameterLimit) {
                concepts = executeMappedLookup(source,
                        Arrays.copyOfRange(identifiers, parameterLimit, identifiers.length));
                identifiers = Arrays.copyOfRange(identifiers, 0, parameterLimit);
            }

            var psr = prepareExecuteMappedLookup(identifiers, source);
            var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
            return concepts.addAll(jdbcTemplate.query(psr.getSql(), psr.getSetter(), this.rowMapper))
                    ? concepts : new HashSet<>();
        }
    }

    protected PreparedStatementRenderer prepareExecuteMappedLookup(long[] identifiers, SourceEntity source) {
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var resourcePath = "vocabulary/sql/getMappedSourcecodes.sql";
        return new PreparedStatementRenderer(source, resourcePath, VOCAB_SCHEMA, tqValue, "identifiers", identifiers);
    }

    public Collection<Concept> executeMappedLookup(String sourceKey, ConceptSetExpression conceptSetExpression) {
        var source = sourceService.findBySourceKey(sourceKey);
        var tableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);

        var builder = new ConceptSetExpressionQueryBuilder();
        var query = builder.buildExpressionQuery(conceptSetExpression);
        var psr = new PreparedStatementRenderer(source, query, "vocabulary_database_schema",
                tableQualifier);
        var sqlPath = "vocabulary/sql/getMappedSourcecodes.sql";
        var search = new String[] { "identifiers", VOCAB_SCHEMA };
        var replace = new String[] { psr.getSql(), tableQualifier };
        psr = new PreparedStatementRenderer(source, sqlPath, search, replace, (String[]) null, null);
        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), this.rowMapper);
    }

    public Collection<Concept> executeSearch(SourceEntity source, ConceptSearchRequestDTO search) {
        var psr = prepareExecuteSearch(search, source);
        return sourceService.getSourceJdbcTemplate(source).query(psr.getSql(), psr.getSetter(), this.rowMapper);
    }

    protected PreparedStatementRenderer prepareExecuteSearch(ConceptSearchRequestDTO search, SourceEntity source) {
        // TODO why is escaping this done?
        // TODO this implies that search.query will not be null, and there is a test
        //      below after isLexical() that uses query if it is not empty
        //      or does the serialization somehow put "" there as a value?
        var query = search.query().replace("[", "[[]");

        var resourcePath = search.isLexical()
                ? "vocabulary/sql/searchLexical.sql"
                : "vocabulary/sql/search.sql";
        var searchSql = resourceReader.getText(resourcePath);
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var searchNamesList = new ArrayList<String>();
        var replacementNamesList = new ArrayList<>();
        var variableNameList = new ArrayList<String>();
        var variableValueList = new ArrayList<>();

        searchNamesList.add(VOCAB_SCHEMA);
        replacementNamesList.add(tqValue);

        var filters = "";
        var domainIds = search.domainIds();
        if (domainIds != null && domainIds.length > 0) {
            // lexical search domain filters work slightly differeant than non-lexical
            if (!search.isLexical()) {
                // use domain_ids as-is
                filters += " AND DOMAIN_ID IN (@domainId)";
                variableNameList.add("domainId");
                variableValueList.add(domainIds);
            } else {
                // MEASUREMENT domain is a special case where we want to ensure concept class is 'lab test' or 'procedure'
                var domainClauses = new ArrayList<String>();
                var nonMeasurementDomains = Stream.of(domainIds).filter(s -> !"Measurement".equals(s))
                        .toList().toArray(new String[0]);
                if (nonMeasurementDomains.length > 0) {
                    domainClauses.add("DOMAIN_ID IN (@domainId)");
                    variableNameList.add("domainId");
                    variableValueList.add(nonMeasurementDomains);
                }
                if (Arrays.asList(domainIds).contains("Measurement")) {
                    domainClauses.add(
                            "(DOMAIN_ID = 'Measurement' and LOWER(concept_class_id) in ('lab test', 'procedure'))");
                }
                if (!domainClauses.isEmpty()) {
                    filters += String.format(" AND (%s)", StringUtils.join(domainClauses, " OR "));
                }
            }
        }

        var vocabularyIds = search.vocabularyIds();
        if (vocabularyIds != null && vocabularyIds.length > 0) {
            filters += " AND VOCABULARY_ID IN (@vocabularyId)";
            variableNameList.add("vocabularyId");
            variableValueList.add(vocabularyIds);
        }

        var conceptClassIds = search.conceptClassIds();
        if (conceptClassIds != null && conceptClassIds.length > 0) {
            filters += " AND CONCEPT_CLASS_ID IN (@conceptClassId)";
            variableNameList.add("conceptClassId");
            variableValueList.add(conceptClassIds);
        }

        var invalidReason = search.invalidReason();
        if (invalidReason != null && !StringUtils.isEmpty(invalidReason)) {
            if (invalidReason.equals("V")) {
                filters += " AND INVALID_REASON IS NULL ";
            } else {
                filters += " AND INVALID_REASON = @invalidReason";
                variableNameList.add("invalidReason");
                variableValueList.add(invalidReason.trim());
            }
        }

        var standardConcept = search.standardConcept();
        if (standardConcept != null) {
            if (standardConcept.equals("N")) {
                filters += " AND STANDARD_CONCEPT IS NULL ";
            } else {
                filters += " AND STANDARD_CONCEPT = @standardConcept";
                variableNameList.add("standardConcept");
                variableValueList.add(standardConcept.trim());
            }
        }

        if (search.isLexical()) {
            // 1. Create term variables for the expressions including truncated terms (terms >=8 are truncated to 6 letters
            var searchTerms = Arrays.asList(StringUtils.split(query.toLowerCase(), " "));
            var allTerms = Stream.concat(
                    searchTerms.stream(),
                    searchTerms.stream().filter(i -> i.length() >= 8).map(i -> StringUtils.left(i, 6))
            ).sorted((a, b) -> b.length() - a.length()).toList();
            var termMap = new LinkedHashMap<String, String>();
            for (int i = 0; i < allTerms.size(); i++) {
                termMap.put(String.format("term_%d", i + 1), allTerms.get(i));
            }
            // 2. Create REPLACE expressions to calculate the match ratio
            var replaceExpression = termMap.keySet().stream()
                    .reduce("", (acc, element) -> {
                        return acc.isEmpty() ?
                                String.format("REPLACE(lower(concept_name), '@%s','')", element) // the first iteration
                                : String.format("REPLACE(%s, '@%s','')", acc, element); // the subsequent iterations
                    });
            searchNamesList.add("replace_expression");
            replacementNamesList.add(replaceExpression);

            // 3. Create the set of 'like' expressions for concept name from the terms that are < 8 chars
            var nameFilterList = termMap.keySet().stream()
                    .filter(k -> termMap.get(k).length() < 8)
                    .map(k -> String.format("lower(concept_name) like '%%@%s%%'", k))
                    .collect(Collectors.toList());
            searchNamesList.add("name_filters");
            replacementNamesList.add(StringUtils.join(nameFilterList, " AND "));

            // 4. Create the set of 'like' expressions for concept synonyms
            var synonymFilterList = termMap.keySet().stream()
                    .filter(k -> termMap.get(k).length() < 8)
                    .map(k -> String.format("lower(concept_synonym_name) like '%%@%s%%'", k))
                    .collect(Collectors.toList());
            searchNamesList.add("synonym_filters");
            replacementNamesList.add(StringUtils.join(synonymFilterList, " AND "));

            // 5. Create name-value pairs for each term parameter
            for (Map.Entry<String, String> entry : termMap.entrySet()) {
                variableNameList.add(entry.getKey());
                variableValueList.add(entry.getValue());
            }
        } else {
            if (!query.isEmpty()) {
                var queryFilter = "LOWER(CONCEPT_NAME) LIKE '%@query%' or LOWER(CONCEPT_CODE) LIKE '%@query%'";
                if (StringUtils.isNumeric(query)) {
                    queryFilter += " or CONCEPT_ID = CAST(@query as int)";
                }
                filters += " AND (" + queryFilter + ")";
                variableNameList.add("query");
                variableValueList.add(query.toLowerCase());
            }
        }
        searchSql = StringUtils.replace(searchSql, "@filters", filters);

        var searchNames = searchNamesList.toArray(new String[0]);
        var replacementNames = replacementNamesList.toArray(new String[0]);

        var variableNames = variableNameList.toArray(new String[variableNameList.size()]);
        var variableValues = variableValueList.toArray(new Object[variableValueList.size()]);

        return new PreparedStatementRenderer(
                source,
                searchSql, searchNames,
                replacementNames,
                variableNames,
                variableValues);
    }

    public Collection<Concept> executeSearch(SourceEntity source, String query, int rows) {
        var search = new ConceptSearchRequestDTO(
                query,
                new String[0],
                new String[0],
                null,
                null,
                new String[0],
                false);
        return executeSearch(source, search);
    }

    public Concept getConcept(SourceEntity source, final long id) {
        var sqlPath = "vocabulary/sql/getConcept.sql";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var psr = new PreparedStatementRenderer(source, sqlPath, VOCAB_SCHEMA, tqValue, "id",
                whitelist(id));

        Concept concept;
        try {
            // TODO queryForObject is deprecated
            concept = sourceService.getSourceJdbcTemplate(source).queryForObject(psr.getSql(), psr.getOrderedParams(),
                    this.rowMapper);
        } catch (EmptyResultDataAccessException e) {
            log.error("Request for conceptId={} resulted in 0 results", id);
            throw new NotFoundException(String.format("There is no concept with id = %d.", id));
        }
        return concept;
    }

    public Collection<RelatedConcept> getRelatedConcepts(SourceEntity source, final Long id) {
        final Map<Long, RelatedConcept> concepts = new HashMap<>();
        var sqlPath = "vocabulary/sql/getRelatedConcepts.sql";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var psr = new PreparedStatementRenderer(source, sqlPath, VOCAB_SCHEMA, tqValue, "id",
                whitelist(id));

        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        jdbcTemplate.query(psr.getSql(), psr.getSetter(), (RowMapper<Void>) (resultSet, arg1) -> {
            addRelationships(concepts, resultSet);
            return null;
        });

        return concepts.values();
    }

    public Collection<RelatedConcept> getConceptAncestorAndDescendant(SourceEntity source, final Long id) {
        final Map<Long, RelatedConcept> concepts = new HashMap<>();
        var sqlPath = "vocabulary/sql/getConceptAncestorAndDescendant.sql";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var psr = new PreparedStatementRenderer(source, sqlPath, VOCAB_SCHEMA, tqValue, "id",
                whitelist(id));

        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        jdbcTemplate.query(psr.getSql(), psr.getSetter(), (RowMapper<Void>) (resultSet, arg1) -> {
            addRelationships(concepts, resultSet);
            return null;
        });

        return concepts.values();
    }

    public Collection<RelatedConcept> getCommonAncestors(SourceEntity source, Object[] identifiers) {
        var psr = prepareGetCommonAncestors(identifiers, source);
        final Map<Long, RelatedConcept> concepts = new HashMap<>();

        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        jdbcTemplate.query(psr.getSql(), psr.getSetter(), (RowMapper<Void>) (resultSet, arg1) -> {
                    addRelationships(concepts, resultSet);
                    return null;
                }
        );

        return concepts.values();
    }

    protected PreparedStatementRenderer prepareGetCommonAncestors(Object[] identifiers, SourceEntity source) {
        var sqlPath = "vocabulary/sql/getCommonAncestors.sql";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);

        return new PreparedStatementRenderer(source, sqlPath, VOCAB_SCHEMA, tqValue,
                new String[] { "conceptIdentifierList", "conceptIdentifierListLength" },
                new Object[] { identifiers, identifiers.length });
    }

    public Collection<Long> resolveConceptSetExpression(SourceEntity source, ConceptSetExpression conceptSetExpression) {
        var psr = new ConceptSetStrategy(conceptSetExpression).prepareStatement(source, null);
        final ArrayList<Long> identifiers = new ArrayList<>();

        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        jdbcTemplate.query(psr.getSql(), psr.getSetter(), rs -> {
            identifiers.add(rs.getLong("CONCEPT_ID"));
        });

        return identifiers;
    }

    public Integer countIncludedConceptSets(SourceEntity source, ConceptSetExpression conceptSetExpression) {
        var query = new ConceptSetStrategy(conceptSetExpression).prepareStatement(source,
                sql -> "select count(*) from (" + sql + ") Q;").getSql();

        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        return jdbcTemplate.query(query, rs -> rs.next() ? rs.getInt(1) : 0);
    }

    public String getConceptSetExpressionSQL(@RequestBody ConceptSetExpression conceptSetExpression) {
        var builder = new ConceptSetExpressionQueryBuilder();
        return builder.buildExpressionQuery(conceptSetExpression);
    }

    public Collection<RelatedConcept> getDescendantConcepts(SourceEntity source, final Long id) {
        final Map<Long, RelatedConcept> concepts = new HashMap<>();
        var sqlPath = "vocabulary/sql/getDescendantConcepts.sql";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var psr = new PreparedStatementRenderer(source, sqlPath, VOCAB_SCHEMA, tqValue, "id",
                whitelist(id));

        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        jdbcTemplate.query(psr.getSql(), psr.getSetter(), (RowMapper<Void>) (resultSet, arg1) -> {
            addRelationships(concepts, resultSet);
            return null;
        });

        return concepts.values();
    }

    public Collection<Domain> getDomains(SourceEntity source) {
        var tableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var sqlPath = "vocabulary/sql/getDomains.sql";
        var psr = new PreparedStatementRenderer(source, sqlPath, VOCAB_SCHEMA, tableQualifier);

        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        return jdbcTemplate.query(psr.getSql(), psr.getSetter(), (resultSet, arg1) -> {
            final var domain = new Domain();
            domain.domainId = resultSet.getString("DOMAIN_ID");
            domain.domainName = resultSet.getString("DOMAIN_NAME");
            domain.domainConceptId = resultSet.getLong("DOMAIN_CONCEPT_ID");
            return domain;
        });
    }

    public Collection<Vocabulary> getVocabularies(SourceEntity source) {
        var sqlPath = "vocabulary/sql/getVocabularies.sql";
        var tableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var psr = new PreparedStatementRenderer(source, sqlPath, VOCAB_SCHEMA, tableQualifier);

        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        return jdbcTemplate.query(psr.getSql(), psr.getSetter(), (resultSet, arg1) -> {
            final var vocabulary = new Vocabulary();
            vocabulary.vocabularyId = resultSet.getString("VOCABULARY_ID");
            vocabulary.vocabularyName = resultSet.getString("VOCABULARY_NAME");
            vocabulary.vocabularyReference = resultSet.getString("VOCABULARY_REFERENCE");
            vocabulary.vocabularyVersion = resultSet.getString("VOCABULARY_VERSION");
            vocabulary.vocabularyConceptId = resultSet.getLong("VOCABULARY_CONCEPT_ID");
            return vocabulary;
        });
    }

    private void addRelationships(final Map<Long, RelatedConcept> concepts, final ResultSet resultSet)
            throws SQLException {
        final Long concept_id = resultSet.getLong("CONCEPT_ID");
        if (!concepts.containsKey(concept_id)) {
            final var concept = new RelatedConcept();
            concept.conceptId = concept_id;
            concept.conceptCode = resultSet.getString("CONCEPT_CODE");
            concept.conceptName = resultSet.getString("CONCEPT_NAME");
            concept.standardConcept = resultSet.getString("STANDARD_CONCEPT");
            concept.invalidReason = resultSet.getString("INVALID_REASON");
            concept.vocabularyId = resultSet.getString("VOCABULARY_ID");
            concept.validStartDate = resultSet.getDate("VALID_START_DATE");
            concept.validEndDate = resultSet.getDate("VALID_END_DATE");
            concept.conceptClassId = resultSet.getString("CONCEPT_CLASS_ID");
            concept.domainId = resultSet.getString("DOMAIN_ID");

            final var relationship = new ConceptRelationship();
            relationship.relationshipName = resultSet.getString("RELATIONSHIP_NAME");
            relationship.relationshipDistance = resultSet.getInt("RELATIONSHIP_DISTANCE");
            concept.relationships.add(relationship);

            concepts.put(concept_id, concept);
        } else {
            final var relationship = new ConceptRelationship();
            relationship.relationshipName = resultSet.getString("RELATIONSHIP_NAME");
            relationship.relationshipDistance = resultSet.getInt("RELATIONSHIP_DISTANCE");
            concepts.get(concept_id).relationships.add(relationship);
        }
    }

    public VocabularyInfo getInfo(String sourceKey) {
        if (vocabularyInfoCache == null) {
            vocabularyInfoCache = new Hashtable<>();
        }

        if (!vocabularyInfoCache.containsKey(sourceKey)) {
            final VocabularyInfo info = new VocabularyInfo();
            var source = sourceService.findBySourceKey(sourceKey);
            var sqlPath = "vocabulary/sql/getInfo.sql";
            var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
            var psr = new PreparedStatementRenderer(source, sqlPath, VOCAB_SCHEMA, tqValue);
            info.dialect = source.getSourceDialect();

            var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
            vocabularyInfoCache.put(sourceKey,
                    // TODO queryForObject is deprecated
                    jdbcTemplate.queryForObject(psr.getSql(), psr.getOrderedParams(),
                            (resultSet, arg1) -> {
                                info.version = resultSet.getString("VOCABULARY_VERSION");
                                return info;
                            }));
        }

        return vocabularyInfoCache.get(sourceKey);
    }

    public void clearVocabularyInfoCache() {
        vocabularyInfoCache = null;
    }

    public Collection<Concept> getDescendantOfAncestorConcepts(SourceEntity source, DescendentOfAncestorSearch search) {
        var psr = prepareGetDescendantOfAncestorConcepts(search, source);
        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        return jdbcTemplate.query(psr.getSql(), psr.getSetter(), this.rowMapper);
    }

    protected PreparedStatementRenderer prepareGetDescendantOfAncestorConcepts(
            DescendentOfAncestorSearch search,
            SourceEntity source) {
        var sqlPath = "vocabulary/sql/getDescendentOfAncestorConcepts.sql";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var names = new String[] { "id", "ancestorVocabularyId", "ancestorClassId", "siblingVocabularyId",
                "siblingClassId" };
        var values = new Object[] { Integer.valueOf(search.conceptId), search.ancestorVocabularyId,
                search.ancestorClassId, search.siblingVocabularyId, search.siblingClassId };

        return new PreparedStatementRenderer(source, sqlPath, VOCAB_SCHEMA, tqValue, names, values);
    }

    public Collection<Concept> getRelatedConcepts(SourceEntity source, RelatedConceptSearch search) {
        var psr = prepareGetRelatedConcepts(search, source);
        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        return jdbcTemplate.query(psr.getSql(), psr.getSetter(), this.rowMapper);
    }

    protected PreparedStatementRenderer prepareGetRelatedConcepts(RelatedConceptSearch search, SourceEntity source) {
        var tableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var resourcePath = "vocabulary/sql/getRelatedConceptsFiltered.sql";

        var filtersSql = "";
        if (search.vocabularyId != null && search.vocabularyId.length > 0) {
            filtersSql += "VOCABULARY_ID IN (@vocabularyId)";
        }
        if (search.conceptClassId != null && search.conceptClassId.length > 0) {
            if (!filtersSql.isEmpty()) {
                filtersSql += " AND ";
            }
            filtersSql += "CONCEPT_CLASS_ID IN (@conceptClassId)";
        }
        String[] searchStrings = { VOCAB_SCHEMA, "filters" };
        String[] replacementStrings = { tableQualifier, filtersSql };

        String[] varNames = { "vocabularyId", "conceptClassId", "conceptList" };
        Object[] varValues = { search.vocabularyId, search.conceptClassId, search.conceptId };

        return new PreparedStatementRenderer(
                source,
                resourcePath,
                searchStrings,
                replacementStrings,
                varNames,
                varValues);
    }

    public Collection<RelatedConcept> getDescendantConceptsByList(SourceEntity source, String[] conceptList) {
        final Map<Long, RelatedConcept> concepts = new HashMap<>();
        var psr = prepareGetDescendantConceptsByList(conceptList, source);

        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        jdbcTemplate.query(psr.getSql(), psr.getSetter(), (RowMapper<Void>) (resultSet, arg1) -> {
            addRelationships(concepts, resultSet);
            return null;
        });

        return concepts.values();
    }

    protected PreparedStatementRenderer prepareGetDescendantConceptsByList(String[] conceptList, SourceEntity source) {
        var sqlPath = "vocabulary/sql/getDescendantConceptsMultipleConcepts.sql";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var conceptArray = Arrays.stream(conceptList).map(NumberUtils::toInt).collect(Collectors.toList());
        return new PreparedStatementRenderer(source, sqlPath, VOCAB_SCHEMA, tqValue, "id", conceptArray.toArray());
    }

    public Collection<RecommendedConcept> getRecommendedConceptsByList(String sourceKey, long[] conceptList) {
        if (conceptList.length == 0) {
            return new ArrayList<>(); // empty list of recommendations
        }
        try {
            final Map<Long, RecommendedConcept> concepts = new HashMap<>();
            var source = sourceService.findBySourceKey(sourceKey);
            var psr = prepareGetRecommendedConceptsByList(conceptList, source);

            var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
            jdbcTemplate.query(psr.getSql(), psr.getSetter(), (RowMapper<Void>) (resultSet, i) -> {
                addRecommended(concepts, resultSet);
                return null;
            });

            return concepts.values();
        } catch (Exception e) {
            if (e.getCause().getMessage().contains("concept_recommended")) {
                throw new ConceptRecommendedNotInstalledException();
            }
            throw e;
        }
    }

    protected PreparedStatementRenderer prepareGetRecommendedConceptsByList(long[] conceptList, SourceEntity source) {
        var sqlPath = "vocabulary/sql/getRecommendConcepts.sql";
        var tqValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        return new PreparedStatementRenderer(source, sqlPath, VOCAB_SCHEMA, tqValue, "conceptList", conceptList);
    }

    private void addRecommended(Map<Long, RecommendedConcept> concepts, ResultSet resultSet) throws SQLException {
        final Long concept_id = resultSet.getLong("CONCEPT_ID");

        if (!concepts.containsKey(concept_id)) {
            // use rowmapper to read the standard concept columns and re-serialize into RecommendedConcept
            final var concept = Utils.deserialize(
                    Utils.serialize(this.rowMapper.mapRow(resultSet, 0), Boolean.TRUE), RecommendedConcept.class);
            concept.relationships = new ArrayList<>();
            concepts.put(concept_id, concept);
        }
        var concept = concepts.get(concept_id);
        concept.relationships.add(resultSet.getString("RELATIONSHIP_ID"));
    }

    public Collection<ConceptSetComparison> compareConceptSets(SourceEntity source, ConceptSetExpression[] conceptSetExpressionList) throws Exception {
        if (conceptSetExpressionList.length != 2) {
            throw new IllegalArgumentException("You must specify two concept set expressions in order to use this method.");
        }
        var vocabSchema = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);

        // Get the comparison script
        var sql_statement = resourceReader.getText("vocabulary/sql/compareConceptSets.sql");

        // Get the queries that represent the ConceptSetExpressions that were passed
        // into the function
        var builder = new ConceptSetExpressionQueryBuilder();
        var cs1Query = builder.buildExpressionQuery(conceptSetExpressionList[0]);
        var cs2Query = builder.buildExpressionQuery(conceptSetExpressionList[1]);

        // Insert the queries into the overall comparison script
        sql_statement = SqlRender.renderSql(sql_statement, new String[] { "cs1_expression", "cs2_expression" },
                new String[] { cs1Query, cs2Query });
        sql_statement = SqlRender.renderSql(sql_statement, new String[] { "vocabulary_database_schema" },
                new String[] { vocabSchema });
        sql_statement = SqlTranslate.translateSql(sql_statement, source.getSourceDialect());

        // Execute the query
        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        return jdbcTemplate.query(sql_statement,
                CONCEPT_SET_COMPARISON_ROW_MAPPER);
    }

    public Collection<ConceptSetComparison> compareConceptSetsCsv(final String sourceKey, final CompareArbitraryDto dto) throws Exception {
        final var csExpressionList = dto.compareTargets;
        if (csExpressionList.length != 2) {
            throw new IllegalArgumentException("You must specify two concept set expressions in order to use this method.");
        }

        final var returnVal = conceptSetCompareService.compareConceptSets(sourceKey, dto);

        // maps for items "not found in DB from input1", "not found in DB from input2"
        final var input1Ex = ExpressionFileUtils.toExclusionMap(
                csExpressionList[0].items, returnVal);
        final var input2ex = ExpressionFileUtils.toExclusionMap(
                csExpressionList[1].items, returnVal);

        // compare/combine exclusion maps and add the result to the output
        returnVal.addAll(ExpressionFileUtils.combine(input1Ex, input2ex));

        // concept names to display mismatches
        final var names = ExpressionFileUtils.toNamesMap(csExpressionList[0].items,
                csExpressionList[1].items);
        returnVal.forEach(item -> {
            final var name = names.get(ExpressionFileUtils.getKey(item));
            item.nameMismatch = name != null && !name.equals(item.conceptName);
        });

        return returnVal;
    }

    public ConceptSetOptimizationResult optimizeConceptSet(SourceEntity source, ConceptSetExpression conceptSetExpression) throws Exception {
        String tableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);

        // Get the optimization script
        var sql_statement = resourceReader.getText("vocabulary/sql/optimizeConceptSet.sql");

        // Find all of the concepts that should be considered for optimization
        // Create a hashtable to hold all of the contents of the ConceptSetExpression
        // for use later
        var allConceptSetItems = new Hashtable<Integer, ConceptSetExpression.ConceptSetItem>();
        var includedConcepts = new ArrayList<Integer>();
        var descendantConcepts = new ArrayList<Integer>();
        var allOtherConcepts = new ArrayList<Integer>();
        for (var item : conceptSetExpression.items) {
            allConceptSetItems.put(item.concept.conceptId.intValue(), item);
            if (!item.isExcluded) {
                includedConcepts.add(item.concept.conceptId.intValue());
                if (item.includeDescendants) {
                    descendantConcepts.add(item.concept.conceptId.intValue());
                }
            } else {
                allOtherConcepts.add(item.concept.conceptId.intValue());
            }
        }

        // If no descendant concepts are specified, initialize this field to use concept_id = 0 so the query will work properly
        if (descendantConcepts.isEmpty())
            descendantConcepts.add(0);

        var allConceptsList = includedConcepts.stream().map(Object::toString).collect(Collectors.joining(", "));
        var descendantConceptsList = descendantConcepts.stream().map(Object::toString)
                .collect(Collectors.joining(", "));

        sql_statement = SqlRender.renderSql(sql_statement,
                new String[] { "allConcepts", "descendantConcepts", "vocab_database_schema" },
                new String[] { allConceptsList, descendantConceptsList, tableQualifier });
        sql_statement = SqlTranslate.translateSql(sql_statement, source.getSourceDialect());

        // Execute the query to obtain a result set that contains the
        // most optimized version of the concept set. Then, using these results,
        // construct a new ConceptSetExpression object that only contains the
        // concepts that were identified as optimal to achieve the same definition
        var returnVal = new ConceptSetOptimizationResult();
        var optimzedExpressionItems = new ArrayList<ConceptSetExpression.ConceptSetItem>();
        var removedExpressionItems = new ArrayList<ConceptSetExpression.ConceptSetItem>();

        var jdbcTemplate = sourceService.getSourceJdbcTemplate(source);
        var rows = jdbcTemplate.queryForList(sql_statement);
        for (var rs : rows) {
            Integer conceptId = Integer.parseInt(rs.get("concept_id").toString());
            var removed = String.valueOf(rs.get("removed"));
            var csi = allConceptSetItems.get(conceptId);
            if (removed.equals("0")) {
                optimzedExpressionItems.add(csi);
            } else {
                removedExpressionItems.add(csi);
            }
        }
        // Re-add back the other concepts that are not considered
        // as part of the optimizatin process
        for (var conceptId : allOtherConcepts) {
            var csi = allConceptSetItems.get(conceptId);
            optimzedExpressionItems.add(csi);
        }
        returnVal.optimizedConceptSet.items = optimzedExpressionItems.toArray(
                new ConceptSetExpression.ConceptSetItem[optimzedExpressionItems.size()]);
        returnVal.removedConceptSet.items = removedExpressionItems.toArray(
                new ConceptSetExpression.ConceptSetItem[removedExpressionItems.size()]);

        return returnVal;
    }
}
