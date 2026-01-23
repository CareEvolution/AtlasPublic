package org.ohdsi.webapi.authorization.service;

import org.ohdsi.webapi.authorization.entity.UserEntity;
import org.ohdsi.webapi.authorization.model.UserOrigin;
import org.ohdsi.webapi.authorization.usercache.UserCache;
import org.ohdsi.webapi.authorization.dto.PermissionDTO;
import org.ohdsi.webapi.authorization.dto.RoleDTO;
import org.ohdsi.webapi.authorization.dto.UserMeDTO;
import org.ohdsi.webapi.authorization.repository.UserRepository;
import org.ohdsi.webapi.authorization.dto.UserDTO;
import org.ohdsi.webapi.exceptions.NoSuchConceptException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserCache userCache;

    public UserService(
            UserRepository userRepository,
            UserCache userCache) {
        this.userRepository = userRepository;
        this.userCache = userCache;
    }

    @Transactional
    public UserEntity createUser(String login, String name, UserOrigin userOrigin) {
        var userEntity = new UserEntity();
        userEntity.setLogin(login);
        userEntity.setName(name);
        userEntity.setOrigin(userOrigin);

        var savedUserEntity = userRepository.save(userEntity);

        return savedUserEntity;
    }

    public UserDTO getUser(Long userId) {
        var userProfile = userCache.getUserProfile(userId);
        return new UserDTO(userProfile.getUserId(), userProfile.getLogin(), userProfile.getName());
    }

    public UserDTO getUser(String login) {
        var userProfile = userCache.getUserProfile(login);
        return new UserDTO(userProfile.getUserId(), userProfile.getLogin(), userProfile.getName());
    }

    public UserMeDTO getUserMe(Long userId) {
        var userProfile = userCache.getUserProfile(userId);

        var me = new UserMeDTO(
                userProfile.getUserId(),
                userProfile.getLogin(),
                userProfile.getName());
        return me;
    }

    public List<PermissionDTO> getUserPermissions(Long userId) {
        var userProfile = userCache.getUserProfile(userId);
        var permissions = new ArrayList<>(userProfile.getPermissions().values());
        permissions.sort(Comparator.comparing(PermissionDTO::permission));
        return permissions;
    }

    public List<RoleDTO> getUserRoles(Long userId) {
        var userProfile = userCache.getUserProfile(userId);

        var roles = new ArrayList<>(userProfile.getRoles().values());
        roles.sort(Comparator.comparing(RoleDTO::name));
        return roles;
    }

    @Transactional
    public List<UserDTO> getUsers() {
        var userEntities = this.userRepository.findAll();

        var users = new ArrayList<UserDTO>();
        for (var userEntity : userEntities) {
            var user = new UserDTO(userEntity.getId(), userEntity.getLogin(), userEntity.getName());
            users.add(user);
        }
        return users;
    }

    @Transactional
    public void setUserLastViewedNotificationTime(Long userId, Date lastViewedAt) {
        var optionalUserEntity = userRepository.findById(userId);
        if (optionalUserEntity.isEmpty()) {
            throw new NoSuchConceptException(String.format("User %d does not exist", userId));
        }

        var userEntity = optionalUserEntity.get();
        userEntity.setLastViewedNotificationsTime(lastViewedAt);
        userRepository.save(userEntity);

        // No need to clear user cache, we've updated the database and will now update
        // this user's last viewed notifications time.
        var userProfile = userCache.getUserProfile(userId);
        userProfile.setLastViewedNotificationsTime(lastViewedAt);
    }
}
