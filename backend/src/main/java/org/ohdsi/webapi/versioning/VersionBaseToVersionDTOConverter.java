package org.ohdsi.webapi.versioning;

import org.ohdsi.webapi.authorization.converter.UserEntityToUserDTOConverter;
import org.ohdsi.webapi.versioning.domain.VersionBase;
import org.ohdsi.webapi.versioning.dto.VersionDTO;

public class VersionBaseToVersionDTOConverter {
    public static VersionDTO convert(VersionBase source) {
        VersionDTO target = new VersionDTO();
        target.setComment(source.getComment());
        target.setAssetId(source.getAssetId());
        target.setVersion(source.getVersion());
        target.setArchived(source.isArchived());
        target.setCreatedBy(UserEntityToUserDTOConverter.convert(source.getCreatedBy()));
        target.setCreatedDate(source.getCreatedDate());
        return target;
    }

}
