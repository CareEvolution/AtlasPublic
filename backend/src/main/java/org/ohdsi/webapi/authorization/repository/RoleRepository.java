package org.ohdsi.webapi.authorization.repository;

import org.ohdsi.webapi.authorization.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

  RoleEntity findByNameAndSystemRole(String name, Boolean isSystem);

  List<RoleEntity> findByNameIgnoreCaseContaining(String roleSearch);

  Iterable<RoleEntity> findAllBySystemRoleTrue();

  @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM RoleEntity r WHERE r.name = ?1")
  boolean existsByName(String roleName);

  @Query(
    """
    SELECT r
    FROM RoleEntity r
    JOIN RolePermissionEntity rp ON r.id = rp.role.id
    JOIN PermissionEntity p ON rp.permission.id = p.id
    WHERE p.value IN :permissions
    GROUP BY r.id, r.name, r.systemRole
    """
  )
  List<RoleEntity> findAllRolesHavingPermissions(@Param("permissions") List<String> permissions);

  void deleteByNameAndSystemRole(String name, Boolean isSystem);
}
