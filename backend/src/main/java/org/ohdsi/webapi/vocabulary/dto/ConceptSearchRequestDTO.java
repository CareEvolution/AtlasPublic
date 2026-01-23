package org.ohdsi.webapi.vocabulary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConceptSearchRequestDTO (
    @JsonProperty("QUERY")
    String query,

    @JsonProperty("DOMAIN_ID")
    String[] domainIds,

    @JsonProperty("VOCABULARY_ID")
    String[] vocabularyIds,

    @JsonProperty("STANDARD_CONCEPT")
    String standardConcept,

    @JsonProperty("INVALID_REASON")
    String invalidReason,

    @JsonProperty("CONCEPT_CLASS_ID")
    String[] conceptClassIds,

    @JsonProperty("IS_LEXICAL")
    boolean isLexical){}
