package org.ohdsi.webapi.tag.dto;

import java.util.List;

public record AssetGroupDTO(
        List<Integer> cohorts,
        List<Integer> conceptSets,
        List<Long> characterizations
) {}
