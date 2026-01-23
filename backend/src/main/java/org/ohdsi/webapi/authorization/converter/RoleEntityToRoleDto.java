package org.ohdsi.webapi.authorization.converter;

import org.ohdsi.webapi.authorization.dto.RoleDTO;
import org.ohdsi.webapi.authorization.entity.RoleEntity;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class RoleEntityToRoleDto implements Converter<RoleEntity, RoleDTO> {
    @Override
    public RoleDTO convert(@NonNull RoleEntity roleEntity) {
        return new RoleDTO(roleEntity.getId(), roleEntity.getName(), roleEntity.isSystemRole());
    }
}
