package org.ohdsi.webapi.cohortdefinition.converter;

import org.ohdsi.webapi.authorization.converter.UserEntityToUserDTOConverter;
import org.ohdsi.webapi.cohortdefinition.CohortGenerationInfo;
import org.ohdsi.webapi.cohortdefinition.dto.CohortGenerationInfoDTO;

public class CohortGenerationInfoToCohortGenerationInfoDTOConverter {
    public static CohortGenerationInfoDTO convert(CohortGenerationInfo info) {
        final CohortGenerationInfoDTO dto = new CohortGenerationInfoDTO();

        dto.setIsCanceled(info.isCanceled());
        dto.setCreatedBy(UserEntityToUserDTOConverter.convert(info.getCreatedBy()));
        dto.setExecutionDuration(info.getExecutionDuration());
        dto.setFailMessage(info.getFailMessage());
        dto.setId(info.getId());
        dto.setPersonCount(info.getPersonCount());
        dto.setRecordCount(info.getRecordCount());
        dto.setStartTime(info.getStartTime());
        dto.setStatus(info.getStatus());
        dto.setIsValid(info.isIsValid());

        return dto;
    }
}
