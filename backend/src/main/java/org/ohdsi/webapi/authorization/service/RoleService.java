package org.ohdsi.webapi.authorization.service;

import jakarta.ws.rs.NotFoundException;
import org.ohdsi.webapi.authorization.model.AccessType;
import org.ohdsi.webapi.authorization.repository.*;
import org.ohdsi.webapi.authorization.usercache.UserCache;
import org.ohdsi.webapi.authorization.dto.PermissionDTO;
import org.ohdsi.webapi.authorization.dto.RoleDTO;
import org.ohdsi.webapi.authorization.dto.UserDTO;
import org.ohdsi.webapi.authorization.model.UserOrigin;
import org.ohdsi.webapi.authorization.entity.RoleEntity;
import org.ohdsi.webapi.authorization.entity.RolePermissionEntity;
import org.ohdsi.webapi.authorization.entity.UserEntity;
import org.ohdsi.webapi.authorization.entity.UserRoleEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleService {
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RoleRepository roleRepository;
    private final UserCache userCache;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public RoleService(
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            RoleRepository roleRepository,
            UserCache userCache,
            UserRepository userRepository,
            UserRoleRepository userRoleRepository) {
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.roleRepository = roleRepository;
        this.userCache = userCache;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional
    public void addPermissionsToRole(Long roleId, String plusSignSeparatedPermissionIds) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role id must be specified");
        }

        var authorizationsChanged = false;

        var roleEntity = roleRepository.findById(roleId)
            .orElseThrow(() -> new NotFoundException(String.format("Role %d does not exist", roleId)));

        var ids = plusSignSeparatedPermissionIds.split("\\+");
        for (var id : ids) {
            var permissionId = Long.parseLong(id);
            var permissionEntity = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new NotFoundException(String.format("Permission %d does not exist", permissionId)));

            var roleToPermissionEntity = rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId);
            if (roleToPermissionEntity == null) {
                var rolePermissionEntity = new RolePermissionEntity();
                rolePermissionEntity.setRole(roleEntity);
                rolePermissionEntity.setPermission(permissionEntity);
                rolePermissionRepository.saveAndFlush(rolePermissionEntity);
                roleEntity.getRolePermissions().add(rolePermissionEntity);
                permissionEntity.getRolePermissions().add(rolePermissionEntity);
                permissionRepository.saveAndFlush(permissionEntity);
                authorizationsChanged = true;
            }
        }

        if (authorizationsChanged) {
            roleRepository.saveAndFlush(roleEntity);
            userCache.clear();
        }
    }

    @Transactional
    public void addRoleToUser(Long userID, RoleEntity role) {
        if (userID == null) {
            throw new IllegalArgumentException("User id must be specified");
        }
        var userEntity = userRepository.findById(userID).
            orElseThrow(() -> new NotFoundException(String.format("User %d does not exist", userID)));
        addRoleToUser(userEntity, role);
    }

    @Transactional
    public void addRoleToUser(UserEntity user, RoleEntity role) {

        var userRoleEntity = userRoleRepository.findByUserIdAndRoleId(user.getId(), role.getId());
        if (userRoleEntity == null) {
            var newUserRoleEntity = new UserRoleEntity();
            newUserRoleEntity.setUser(user);
            newUserRoleEntity.setRole(role);
            newUserRoleEntity.setOrigin(UserOrigin.SYSTEM);
            userRoleRepository.saveAndFlush(newUserRoleEntity);
            user.getUserRoles().add(newUserRoleEntity);
            userRepository.saveAndFlush(user);
            role.getUserRoles().add(newUserRoleEntity);
            roleRepository.saveAndFlush(role);
            userCache.clear();
        }
    }

    @Transactional
    public void addUsersToRole(Long roleId, String plusSignSeparatedUserIds) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role id must be specified");
        }
        var roleEntity = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException(String.format("Role %d does not exist", roleId)));

        var ids = plusSignSeparatedUserIds.split("\\+");
        for (var id : ids) {
            var userId = Long.parseLong(id);
            var userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User %d does not exist", userId)));

            addRoleToUser(userEntity, roleEntity);
        }
    }

    @Transactional
    public RoleEntity createRole(String roleName, Boolean isSystemRole) {
        if (!StringUtils.hasText(roleName)) {
            throw new IllegalArgumentException("Role name must be specified");
        }

        if (roleRepository.existsByName(roleName)) {
            throw new IllegalArgumentException(String.format("Role %s already exists", roleName));
        }

        // Note that we use the Hibernate save() instead of JPA persist().  This is because
        // save() will return the assigned role id.
        var roleEntity = new RoleEntity();
        roleEntity.setName(roleName);
        roleEntity.setSystemRole(isSystemRole);
        var updatedRoleEntity = roleRepository.saveAndFlush(roleEntity);
        return updatedRoleEntity;
    }

    @Transactional
    public void deletePermissionsFromRole(Long roleId, String plusSignSeparatedPermissionIds) {
        var authorizationsChanged = false;

        var ids = plusSignSeparatedPermissionIds.split("\\+");
        for (var id : ids) {
            var permissionId = Long.parseLong(id);
            var rolePermissionEntity = rolePermissionRepository.findByRoleIdAndPermissionId(roleId, permissionId);
            if (rolePermissionEntity != null) {
                rolePermissionRepository.delete(rolePermissionEntity);
                authorizationsChanged = true;
            }
        }

        if (authorizationsChanged) {
            userCache.clear();
        }
    }

    @Transactional
    public void deleteRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role id must be specified");
        }
        roleRepository.deleteById(roleId);
        userCache.clear();
    }

    @Transactional
    public void deleteByNameAndSystemRole(String roleName, Boolean isSystemRole) {
        roleRepository.deleteByNameAndSystemRole(roleName, isSystemRole);
    }

    @Transactional
    public List<RoleDTO> findAllRolesWithNamedEntityReadOrWritePermissions(AccessType accessType, String entityName) {
        var permissions = new ArrayList<String>();
        if (accessType == AccessType.READ) {
            var readPermission = String.format("read:%s", entityName);
            permissions.add(readPermission);
        } else {
            var createPermission = String.format("create:%s", entityName);
            permissions.add(createPermission);
            var deletePermission = String.format("delete:%s", entityName);
            permissions.add(deletePermission);
            var updatePermission = String.format("update:%s", entityName);
            permissions.add(updatePermission);
        }

        var roleEntities = roleRepository.findAllRolesHavingPermissions(permissions);
        if (roleEntities == null || roleEntities.isEmpty()) {
            return Collections.emptyList();
        }

        var roles = new ArrayList<RoleDTO>();
        for (var roleEntity : roleEntities) {
            var role = new RoleDTO(roleEntity.getId(), roleEntity.getName(), roleEntity.isSystemRole());
            roles.add(role);
        }
        return roles;
    }

    @Transactional
    public RoleEntity getRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role id must be specified");
        }
        var roleEntity = roleRepository.findById(roleId)
            .orElseThrow(() -> new NotFoundException(String.format("Role %d does not exist", roleId)));
        return roleEntity;
    }

    @Transactional
    public RoleEntity getRole(String roleName, Boolean isSystemRole) {
        var roleEntity = roleRepository.findByNameAndSystemRole(roleName, isSystemRole);
        if (roleEntity == null) {
            throw new NotFoundException(String.format("Role %s does not exist", roleName));
        }
        return roleEntity;
    }

    @Transactional
    public List<PermissionDTO> getRolePermissions(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Role id must be specified");
        }
        var roleEntity = roleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(String.format("Role %d does not exist", id)));

        var permissionEntities = roleEntity.getRolePermissions()
                .stream()
                .map(RolePermissionEntity::getPermission).toList();

        var permissions = new ArrayList<PermissionDTO>();
        for (var permissionEntity : permissionEntities) {
            var permission = new PermissionDTO(
                    permissionEntity.getId(),
                    permissionEntity.getValue(),
                    permissionEntity.getDescription());
            permissions.add(permission);
        }

        return permissions;
    }

    @Transactional
    public List<RoleDTO> getRoles() {
        return roleRepository.findAll()
            .stream()
            .map(roleEntity -> new RoleDTO(roleEntity.getId(), roleEntity.getName(), roleEntity.isSystemRole()))
            .collect(Collectors.toList());
    }

    @Transactional
    public List<RoleDTO> getRolesThatContainSearchTarget(String searchTarget) {
        var roleEntities = roleRepository.findByNameIgnoreCaseContaining(searchTarget);
        return roleEntities
            .stream()
            .map(roleEntity -> new RoleDTO(roleEntity.getId(), roleEntity.getName(), roleEntity.isSystemRole()))
            .collect(Collectors.toList());
    }

    @Transactional
    public List<UserDTO> getRoleUsers(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role id must be specified");
        }
        var roleEntity = roleRepository.findById(roleId)
            .orElseThrow(() -> new NotFoundException(String.format("Role %d does not exist", roleId)));

        return roleEntity.getUserRoles()
            .stream()
            .map(UserRoleEntity::getUser)
            .map(userEntity -> new UserDTO(userEntity.getId(), userEntity.getLogin(), userEntity.getName()))
            .collect(Collectors.toList());
    }

    @Transactional
    public void removeRoleFromUsers(Long roleId, String plusSignSeparatedUserIds) {
        var authorizationsChanged = false;

        var ids = plusSignSeparatedUserIds.split("\\+");
        for (var id : ids) {
            var userId = Long.parseLong(id);
            var userRoleEntity = userRoleRepository.findByUserIdAndRoleId(userId, roleId);
            if (userRoleEntity != null) {
                userRoleRepository.delete(userRoleEntity);
                authorizationsChanged = true;
            }

            if (authorizationsChanged) {
                userCache.clear();
            }
        }
    }

    @Transactional
    public RoleDTO renameRole(Long id, String newName) {
        if (id == null) {
            throw new IllegalArgumentException("Role id must be specified");
        }
        var roleEntity = roleRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(String.format("Role %d does not exist", id)));

        roleEntity.setName(newName);
        var renamedRoleEntity = roleRepository.save(roleEntity);

        var renamedRoleDto = new RoleDTO(
                renamedRoleEntity.getId(),
                renamedRoleEntity.getName(),
                renamedRoleEntity.isSystemRole());
        return renamedRoleDto;
    }
}
