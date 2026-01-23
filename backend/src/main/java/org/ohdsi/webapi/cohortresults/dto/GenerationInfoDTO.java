package org.ohdsi.webapi.cohortresults.dto;

public record GenerationInfoDTO(
        String sourceKey,
        Integer analysisId,
        Integer progress) {}
