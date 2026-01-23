package org.ohdsi.webapi.authorization.repository;

import java.util.List;

import org.ohdsi.webapi.authorization.entity.PermissionEntity;
import org.ohdsi.webapi.authorization.entity.RoleEntity;
import org.ohdsi.webapi.authorization.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, Long> {

  RolePermissionEntity findByRoleAndPermission(RoleEntity role, PermissionEntity permission);

  RolePermissionEntity findByRoleIdAndPermissionId(Long roleId, Long permissionId);

  List<RolePermissionEntity> findByStatusIgnoreCase(String status);
}
