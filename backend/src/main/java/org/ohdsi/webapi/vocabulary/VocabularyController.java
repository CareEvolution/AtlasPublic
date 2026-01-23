package org.ohdsi.webapi.vocabulary;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ohdsi.circe.vocabulary.ConceptSetExpression;
import org.ohdsi.vocabulary.Concept;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.conceptset.ConceptSetComparison;
import org.ohdsi.webapi.conceptset.ConceptSetOptimizationResult;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.vocabulary.cscompare.CompareArbitraryDto;
import org.ohdsi.webapi.vocabulary.dto.ConceptSearchRequestDTO;
import org.ohdsi.webapi.vocabulary.dto.LookupAncestorsAndDescendantsRequestDTO;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.ws.rs.NotFoundException;

/**
 * REST API for working with the OMOP standardized vocabularies
 *
 */
@RestController
@RequestMapping("vocabulary")
public class VocabularyController {

    private final VocabularyService vocabularyService;
    private final AuthorizationManager authorizationManager;
    private final SourceService sourceService;

    private static final int DEFAULT_SEARCH_ROWS = 20000;
    private static final String DEFAULT_SEARCH_ROWS_STR = "20000";

    public VocabularyController(VocabularyService vocabularyService, AuthorizationManager authorizationManager, SourceService sourceService) {
        this.vocabularyService = vocabularyService;
        this.authorizationManager = authorizationManager;
        this.sourceService = sourceService;
    }

    /**
     * Get the vocabulary version from the vocabulary table using the selected source key
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @return The vocabulary info
     */
    @GetMapping("{sourceKey}/info")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public VocabularyInfo getInfo(@PathVariable("sourceKey") String sourceKey) {
        checkSource(sourceKey);
        return vocabularyService.getInfo(sourceKey);
    }

    /**
     * Search for a concept on the default vocabulary source.
     *
     * @param search
     *         The ConceptSearch parameters
     * @return A collection of concepts
     */
    @PostMapping("search")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> executeSearch(@RequestBody ConceptSearchRequestDTO search) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null) {
            throw new AccessDeniedException("User does not have access to any sources with a vocabulary or cdm daimon.");
        }
        return vocabularyService.executeSearch(source, search);
    }

    /**
     * Search for a concept on the selected source.
     *
     * @param sourceKey
     *         The source key for the concept search
     * @param search
     *         The ConceptSearch parameters
     * @return A collection of concepts
     */
    @PostMapping("{sourceKey}/search")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> executeSearch(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody ConceptSearchRequestDTO search
    ) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.executeSearch(source, search);
    }

    /**
     * Search for a concept based on a query using the selected vocabulary source.
     *
     * @param sourceKey
     *         The source key holding the OMOP vocabulary
     * @param query
     *         The query to use to search for concepts
     * @return A collection of concepts
     */
    @GetMapping("{sourceKey}/search/{query}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> executeSearch(
        @PathVariable("sourceKey") String sourceKey,
        @PathVariable("query") String query
    ) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.executeSearch(source, query, DEFAULT_SEARCH_ROWS);
    }

    /**
     * Search for a concept based on a query using the default vocabulary source. NOTE: This method uses the query as
     * part of the URL query string
     *
     * @param sourceKey
     *         The source key holding the OMOP vocabulary
     * @param query
     *         The query to use to search for concepts
     * @param rows
     *         The number of rows to return.
     * @return A collection of concepts
     */
    @GetMapping("{sourceKey}/search")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> executeSearch(
            @PathVariable("sourceKey") String sourceKey,
            @RequestParam(name="query") String query,
            @RequestParam(name="rows", defaultValue = DEFAULT_SEARCH_ROWS_STR) int rows
    ) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        if (rows <= 0) {
            throw new IllegalArgumentException("The rows parameter must be greater than 0");
        }
        return vocabularyService.executeSearch(source, query, rows);
    }


    /**
     * Search for a concept based on a query using the default vocabulary source. NOTE: This method uses the query as
     * part of the URL and not the query string
     *
     * @param query
     *         The query to use to search for concepts
     * @return A collection of concepts
     */
    @GetMapping("search/{query}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> executeSearch(@PathVariable("query") String query) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null) {
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        }
        return vocabularyService.executeSearch(source, query, DEFAULT_SEARCH_ROWS);
    }

    /**
     * Get a collection of domains from the domain table in the default vocabulary.
     *
     * @return A collection of domains
     */
    @GetMapping("domains")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Domain> getDomains() {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null) {
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        }
        return vocabularyService.getDomains(source);
    }

    /**
     * Get a collection of domains from the domain table in the vocabulary for the the selected source key.
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @return A collection of domains
     */
    @GetMapping("{sourceKey}/domains")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Domain> getDomains(@PathVariable("sourceKey") String sourceKey) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.getDomains(source);
    }

    /**
     * Get a collection of vocabularies from the vocabulary table in the default vocabulary
     *
     * @return A collection of vocabularies
     */
    @GetMapping("vocabularies")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Vocabulary> getVocabularies() {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null) {
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        }
        return vocabularyService.getVocabularies(source);
    }

    /**
     * Get a collection of vocabularies from the vocabulary table in the selected source key.
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @return A collection of vocabularies
     */
    @GetMapping("{sourceKey}/vocabularies")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Vocabulary> getVocabularies(@PathVariable("sourceKey") String sourceKey) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.getVocabularies(source);
    }

    /**
    * Get the descendant concepts for a selected list of concept ids
    *
    * @param conceptList
    *         The list of concept identifiers
    * @return A collection of concepts
    */
    @PostMapping("conceptlist/descendants")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<RelatedConcept> getDescendantConceptsByList(@RequestBody String[] conceptList) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        return vocabularyService.getDescendantConceptsByList(source, conceptList);
    }

    /**
    * Get the related concepts for a list of concept ids using the concept_relationship table
    *
    * @param search
    *         The concept identifiers of interest
    * @return A collection of concepts
    */
    @PostMapping("relatedconcepts")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> getRelatedConcepts(@RequestBody RelatedConceptSearch search) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        return vocabularyService.getRelatedConcepts(source, search);
    }


    /**
     * Get the related concepts for a list of concept ids using the concept_relationship table for the selected source
     * key
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @param search
     *         The concept identifiers of interest
     * @return A collection of concepts
     */
    @PostMapping("{sourceKey}/relatedconcepts")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> getRelatedConcepts(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody RelatedConceptSearch search) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.getRelatedConcepts(source, search);
    }

    /**
     * Get related concepts for the selected concept identifier from the default vocabulary source.
     *
     * @param id
     *         The concept identifier
     * @return A collection of related concepts
     */
    @GetMapping("concept/{id}/related")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<RelatedConcept> getRelatedConcepts(@PathVariable("id") final Long id) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        return vocabularyService.getRelatedConcepts(source, id);
    }

    /**
     * Get related concepts for the selected concept identifier from a source. Related concepts will include those
     * concepts that have a relationship to the selected concept identifier in the CONCEPT_RELATIONSHIP and
     * CONCEPT_ANCESTOR tables.
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @param id
     *         The concept ID to find
     * @return A collection of related concepts
     */
    @GetMapping("{sourceKey}/concept/{id}/related")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<RelatedConcept> getRelatedConcepts(
            @PathVariable("sourceKey") String sourceKey,
            @PathVariable("id") final Long id) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.getRelatedConcepts(source, id);
    }

    /**
     * Get the descendant concepts for a selected list of concept ids for a selected source key
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @param conceptList
     *         The list of concept identifiers
     * @return A collection of concepts
     */
    @PostMapping("{sourceKey}/conceptlist/descendants")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<RelatedConcept> getDescendantConceptsByList(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody String[] conceptList) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.getDescendantConceptsByList(source, conceptList);
    }

    /**
     * Get the recommended concepts for a selected list of concept ids for a selected source key
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @param conceptList
     *         The list of concept identifiers
     * @return A collection of recommended concepts
     */
    @PostMapping("{sourceKey}/lookup/recommended")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<RecommendedConcept> getRecommendedConceptsByList(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody long[] conceptList) {
        checkSource(sourceKey);
        return vocabularyService.getRecommendedConceptsByList(sourceKey, conceptList);
    }

    /**
     * Compares two concept set expressions to find which concepts are shared or unique to each concept set.
     *
     * @param conceptSetExpressionList
     *         Expects a list of exactly 2 concept set expressions
     * @return A collection of concept set comparisons
     */
    @PostMapping("compare")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<ConceptSetComparison> compareConceptSets(
            @RequestBody ConceptSetExpression[] conceptSetExpressionList)
            throws Exception {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");

        return vocabularyService.compareConceptSets(source, conceptSetExpressionList);
    }

    /**
     * Compares two concept set expressions to find which concepts are shared or unique to each concept set for the
     * selected vocabulary source.
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @param conceptSetExpressionList
     *         Expects a list of exactly 2 concept set expressions
     * @return A collection of concept set comparisons
     */
    @PostMapping("{sourceKey}/compare")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<ConceptSetComparison> compareConceptSets(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody ConceptSetExpression[] conceptSetExpressionList) throws Exception {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.compareConceptSets(source, conceptSetExpressionList);
    }

    @PostMapping("{sourceKey}/compare-arbitrary")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<ConceptSetComparison> compareConceptSetsCsv(
            @PathVariable("sourceKey") final String sourceKey,
            @RequestBody final CompareArbitraryDto dto
    ) throws Exception {
        checkSource(sourceKey);
        return vocabularyService.compareConceptSetsCsv(sourceKey, dto);
    }


    /**
     * Optimizes a concept set expressions to find redundant concepts specified in a concept set expression.
     *
     * @param conceptSetExpression
     *         The concept set expression to optimize
     * @return A concept set optimization
     */
    @PostMapping("optimize")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public ConceptSetOptimizationResult optimizeConceptSet(@RequestBody ConceptSetExpression conceptSetExpression) throws Exception {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        return vocabularyService.optimizeConceptSet(source, conceptSetExpression);
    }

    @PostMapping("{sourceKey}/optimize")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public ConceptSetOptimizationResult optimizeConceptSet(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody ConceptSetExpression conceptSetExpression) throws Exception {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.optimizeConceptSet(source, conceptSetExpression);
    }

    /**
    * Get the descendant concepts of the selected ancestor vocabulary and concept class for the selected sibling
    * vocabulary and concept class. It is unclear how this endpoint is used so it may be a candidate to deprecate.
    *
    * @param search
    *         The descendant of ancestor search object
    * @return A collection of concepts
    */
    @PostMapping("descendantofancestor")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> getDescendantOfAncestorConcepts(@RequestBody DescendentOfAncestorSearch search) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        return vocabularyService.getDescendantOfAncestorConcepts(source, search);
    }

    /**
     * Get the descendant concepts of the selected ancestor vocabulary and concept class for the selected sibling
     * vocabulary and concept class. It is unclear how this endpoint is used so it may be a candidate to deprecate.
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @param search
     *         The descendant of ancestor search object
     * @return A collection of concepts
     */
    @PostMapping("{sourceKey}/descendantofancestor")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> getDescendantOfAncestorConcepts(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody DescendentOfAncestorSearch search) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.getDescendantOfAncestorConcepts(source, search);
    }

    /**
     * Get a collection of descendant concepts for the selected concept identifier using the default vocabulary
     *
     * @param id
     *         The concept identifier
     * @return A collection of concepts
     */
    @GetMapping("concept/{id}/descendants")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<RelatedConcept> getDescendantConcepts(@PathVariable("id") final Long id) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        return vocabularyService.getDescendantConcepts(source, id);
    }

    /**
     * Get a collection of descendant concepts for the selected concept identifier using the selected source key
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @param id
     *         The concept identifier
     * @return A collection of concepts
     */
    @GetMapping("{sourceKey}/concept/{id}/descendants")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<RelatedConcept> getDescendantConcepts(
            @PathVariable("sourceKey") String sourceKey,
            @PathVariable("id") final Long id) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.getDescendantConcepts(source, id);
    }

    /**
     * Produces a SQL query to use against your OMOP CDM to create the resolved concept set
     *
     * @param conceptSetExpression
     *         A concept set expression
     * @return SQL Statement as text
     */
    @PostMapping("conceptSetExpressionSQL")
    // No preauth needed since not accessing any source data
    public String getConceptSetExpressionSQL(@RequestBody ConceptSetExpression conceptSetExpression) {
        return vocabularyService.getConceptSetExpressionSQL(conceptSetExpression);
    }


    /**
     * Resolve a concept set expression to get the count of included concepts using the default vocabulary source.
     *
     * @param conceptSetExpression
     *         A concept set expression
     * @return A count of included concepts
     */
    @PostMapping("included-concepts/count")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Integer countIncludedConcepSets(@RequestBody ConceptSetExpression conceptSetExpression) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        return vocabularyService.countIncludedConceptSets(source, conceptSetExpression);
    }

    /**
     * Resolve a concept set expression to get the count of included concepts using the selected vocabulary source.
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @param conceptSetExpression
     *         A concept set expression
     * @return A count of included concepts
     */
    @PostMapping("{sourceKey}/included-concepts/count")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Integer countIncludedConceptSets(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody ConceptSetExpression conceptSetExpression) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.countIncludedConceptSets(source, conceptSetExpression);
    }

    /**
     * Resolve a concept set expression into a collection of concept identifiers using the default vocabulary source.
     *
     * @param conceptSetExpression
     *         A concept set expression
     * @return A collection of concept identifiers
     */
    @PostMapping("resolveConceptSetExpression")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Long> resolveConceptSetExpression(@RequestBody ConceptSetExpression conceptSetExpression) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        return vocabularyService.resolveConceptSetExpression(source, conceptSetExpression);
    }

    /**
     * Resolve a concept set expression into a collection of concept identifiers using the selected vocabulary source.
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @param conceptSetExpression
     *         A concept set expression
     * @return A collection of concept identifiers
     */
    @PostMapping("{sourceKey}/resolveConceptSetExpression")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Long> resolveConceptSetExpression(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody ConceptSetExpression conceptSetExpression) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.resolveConceptSetExpression(source, conceptSetExpression);
    }

    /**
    * Get a list of common ancestor concepts for a selected list of concept identifiers using the default vocabulary
    * source.
    *
    * @param identifiers
    *         An array of concept identifiers
    * @return A collection of related concepts
    */
    @PostMapping("/commonAncestors")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<RelatedConcept> getCommonAncestors(@RequestBody Object[] identifiers) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        return vocabularyService.getCommonAncestors(source, identifiers);
    }

    /**
     * Get a list of common ancestor concepts for a selected list of concept identifiers using the selected vocabulary
     * source.
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @param identifiers
     *         An array of concept identifiers
     * @return A collection of related concepts
     */
    @PostMapping("{sourceKey}/commonAncestors")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<RelatedConcept> getCommonAncestors(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody Object[] identifiers) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.getCommonAncestors(source, identifiers);
    }

    /**
     * Get ancestor and descendant concepts for the selected concept identifier from a source.
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @param id
     *         The concept ID
     * @return A collection of related concepts
     */
    @GetMapping("{sourceKey}/concept/{id}/ancestorAndDescendant")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<RelatedConcept> getConceptAncestorAndDescendant(
            @PathVariable("sourceKey") String sourceKey,
            @PathVariable("id") final Long id) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.getConceptAncestorAndDescendant(source, id);
    }

    /**
     * Get a concept based on the concept identifier from the default vocabulary source
     *
     * @param id
     *         The concept ID to find
     * @return The concept details
     */
    @GetMapping("concept/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Concept getConcept(@PathVariable("id") final long id) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        return vocabularyService.getConcept(source, id);
    }

    /**
     * Get a concept based on the concept identifier from the specified source
     *
     * @param sourceKey
     *         The source containing the vocabulary
     * @param id
     *         The concept ID to find
     * @return The concept details
     */
    @GetMapping("{sourceKey}/concept/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Concept getConcept(
            @PathVariable("sourceKey") final String sourceKey,
            @PathVariable("id") final long id) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.getConcept(source, id);
    }

    /**
     * Get concepts mapped to the selected concept identifiers from a specific source. Find all concepts mapped to the
     * concept identifiers provided. This end-point will check the CONCEPT, CONCEPT_RELATIONSHIP and
     * SOURCE_TO_CONCEPT_MAP tables.
     *
     * @param identifiers
     *         an array of concept identifiers
     * @return A collection of concepts
     */
    @PostMapping("lookup/mapped")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> executeMappedLookup(@RequestBody long[] identifiers) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        return vocabularyService.executeMappedLookup(source, identifiers);
    }

    /**
     * Get concepts mapped to the selected concept identifiers from a specific source. Find all concepts mapped to the
     * concept identifiers provided. This end-point will check the CONCEPT, CONCEPT_RELATIONSHIP and
     * SOURCE_TO_CONCEPT_MAP tables.
     *
     * @param sourceKey
     *         path parameter specifying the source key identifying the source to use for access to the set of
     *         vocabulary tables
     * @param identifiers
     *         an array of concept identifiers
     * @return A collection of concepts
     */
    @PostMapping("{sourceKey}/lookup/mapped")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> executeMappedLookup(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody long[] identifiers) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.executeMappedLookup(source, identifiers);
    }

    /**
    * Get concepts from source codes from the default vocabulary source
    *
    * @param sourcecodes
    *         array of source codes
    * @return A collection of concepts
    */
    @PostMapping("lookup/sourcecodes")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> executeSourcecodeLookup(@RequestBody String[] sourcecodes) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        return vocabularyService.executeSourcecodeLookup(source, sourcecodes);
    }

    /**
     * Get concepts from source codes from a specific source
     *
     * @param sourceKey
     *         path parameter specifying the source key identifying the source to use for access to the set of
     *         vocabulary tables
     * @param sourcecodes
     *         array of source codes
     * @return A collection of concepts
     */
    @PostMapping("{sourceKey}/lookup/sourcecodes")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> executeSourcecodeLookup(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody String[] sourcecodes) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.executeSourcecodeLookup(source, sourcecodes);
    }

    /**
     * Get concepts from concept identifiers (IDs) from the default vocabulary source
     *
     * @param identifiers
     *         an array of concept identifiers
     * @return A collection of concepts
     */
    @PostMapping("lookup/identifiers")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> executeIdentifierLookup(@RequestBody long[] identifiers) {
        var source = sourceService.getPriorityVocabularySource();
        if (source == null)
            throw new AccessDeniedException(
                    "User does not have access to any sources with a vocabulary or cdm daimon.");
        return vocabularyService.executeIdentifierLookup(source, identifiers);
    }

    /**
    * Get concepts from concept identifiers (IDs) from a specific source
    *
    * @param sourceKey
    *         path parameter specifying the source key identifying the source to use for access to the set of
    *         vocabulary tables
    * @param identifiers
    *         an array of concept identifiers
    * @return A collection of concepts
    */
    @PostMapping("{sourceKey}/lookup/identifiers")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Collection<Concept> executeIdentifierLookup(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody long[] identifiers) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.executeIdentifierLookup(source, identifiers);
    }

    /**
     * Calculates the full set of ancestor and descendant concepts for a list of ancestor and descendant concepts
     * specified. This is used by ATLAS when navigating the list of included concepts in a concept set - the full list
     * of ancestors (as defined in the concept set) and the descendants (those concepts included when resolving the
     * concept set) are used to determine which descendant concepts share one or more ancestors.
     *
     * @param ids
     *         Concepts identifiers from concept set
     * @return A map of the form: {id -> list of ascendant ids}
     */
    @PostMapping("{sourceKey}/lookup/identifiers/ancestors")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public Map<Long, List<Long>> calculateAscendants(
            @PathVariable("sourceKey") String sourceKey,
            @RequestBody LookupAncestorsAndDescendantsRequestDTO ids) {
        checkSource(sourceKey);
        var source = sourceService.findBySourceKey(sourceKey);
        if (source == null) {
            throw new NotFoundException("Source with key " + sourceKey + " not found");
        }
        return vocabularyService.calculateAscendants(source, ids);
    }

    private void checkSource(String sourceKey) {
        if (!authorizationManager.currentUserCanRead(sourceKey)) {
            throw new AccessDeniedException("User does not have permission to read this source");
        }
    }

}
