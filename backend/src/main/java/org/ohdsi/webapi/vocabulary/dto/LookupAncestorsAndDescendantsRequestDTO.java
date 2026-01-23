package org.ohdsi.webapi.vocabulary.dto;

import java.util.List;

public record LookupAncestorsAndDescendantsRequestDTO (
        List<Long> ancestors,
        List<Long> descendants){
}
