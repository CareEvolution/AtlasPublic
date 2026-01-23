package org.ohdsi.webapi.authorization.repository;

import java.util.List;

import org.ohdsi.webapi.authorization.entity.RoleEntity;
import org.ohdsi.webapi.authorization.entity.UserEntity;
import org.ohdsi.webapi.authorization.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

  public List<UserRoleEntity> findByUser(UserEntity user);

  public UserRoleEntity findByUserAndRole(UserEntity user, RoleEntity role);

  public List<UserRoleEntity> findByStatusIgnoreCase(String status);

  public List<UserRoleEntity> findByUserId(Long userId);

  public UserRoleEntity findByUserIdAndRoleId(Long userId, Long roleId);

}
