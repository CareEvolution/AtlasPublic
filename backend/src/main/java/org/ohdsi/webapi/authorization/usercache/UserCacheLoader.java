package org.ohdsi.webapi.authorization.usercache;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import jakarta.ws.rs.NotFoundException;

import org.ohdsi.webapi.authorization.dto.PermissionDTO;
import org.ohdsi.webapi.authorization.dto.RoleDTO;
import org.ohdsi.webapi.authorization.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

@Service
public class UserCacheLoader {
    private final UserRepository userRepository;

    public UserCacheLoader(
            UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public BiMap<Long,String> loadIdLoginBiMap() {
        BiMap<Long, String> idLoginBiMap = HashBiMap.create();

        var userEntities = this.userRepository.findAll();
        for (var userEntity : userEntities) {
            idLoginBiMap.put(userEntity.getId(), userEntity.getLogin());
        }
        return idLoginBiMap;
    }

    @Transactional
    public UserProfile loadUserProfile(Long userId) {
        var optionalUserEntity = userRepository.findById(userId);
        if (optionalUserEntity.isEmpty()) {
            throw new NotFoundException(String.format("User with id %d not found", userId));
        }
        var userEntity = optionalUserEntity.get();
        var login = userEntity.getLogin();

        var roles = new HashMap<String,RoleDTO>();
        var permissions = new HashMap<String,PermissionDTO>();

        var userRoles = userEntity.getUserRoles();
        for (var userRole : userRoles) {
            var roleEntity = userRole.getRole();
            var roleName = roleEntity.getName();
            var isSystemRole = roleEntity.isSystemRole();

            var roleDTO = new RoleDTO(roleEntity.getId(), roleName, isSystemRole);
            roles.put(roleName, roleDTO);

            var rolePermissions = roleEntity.getRolePermissions();
            for (var rolePermission : rolePermissions) {
                var permissionEntity = rolePermission.getPermission();
                var permissionValue = permissionEntity.getValue();

                var permissionDTO = new PermissionDTO(
                        permissionEntity.getId(),
                        permissionValue,
                        permissionEntity.getDescription());
                permissions.put(permissionDTO.permission(), permissionDTO);
            }
        }

        return new UserProfile(
                userEntity.getId(),
                login,
                userEntity.getName(),
                userEntity.getLastViewedNotificationsTime(),
                userEntity.getOrigin(),
                roles,
                permissions);
    }
}
