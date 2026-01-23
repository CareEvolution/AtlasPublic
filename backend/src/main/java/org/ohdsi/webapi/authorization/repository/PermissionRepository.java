package org.ohdsi.webapi.authorization.repository;

import org.ohdsi.webapi.authorization.entity.PermissionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

  public PermissionEntity findByValueIgnoreCase(String permission);

  @EntityGraph(attributePaths = {"rolePermissions", "rolePermissions.role"})
  List<PermissionEntity> findByValueLike(String permissionTemplate);

  public void deleteByValueIgnoreCase(String permission);
}
