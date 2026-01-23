package org.ohdsi.webapi.authorization.converter;

import org.ohdsi.webapi.authorization.dto.RoleDTO;
import org.ohdsi.webapi.authorization.entity.RoleEntity;

public class RoleDtoToRoleEntity {
    public static RoleEntity convert(RoleDTO roleDTO) {
        var roleEntity = new RoleEntity();
        roleEntity.setId(roleDTO.id());
        roleEntity.setName(roleDTO.name());
        roleEntity.setSystemRole(roleDTO.isSystemRole());
        return roleEntity;
    }
}
