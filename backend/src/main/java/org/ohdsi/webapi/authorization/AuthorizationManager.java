package org.ohdsi.webapi.authorization;

import org.ohdsi.webapi.authorization.ownershipcache.OwnershipCache;
import org.ohdsi.webapi.authorization.service.UserService;
import org.ohdsi.webapi.commondto.SecuredEntityDTO;
import org.ohdsi.webapi.model.SecuredEntity;
import org.ohdsi.webapi.authorization.converter.UserEntityToUserDTOConverter;
import org.ohdsi.webapi.authorization.dto.UserDTO;
import org.ohdsi.webapi.authorization.model.SecuredEntityType;
import org.ohdsi.webapi.authorization.model.UserOrigin;
import org.ohdsi.webapi.authorization.service.UserRegistrationService;
import org.ohdsi.webapi.authorization.usercache.UserCache;
import org.ohdsi.webapi.authorization.usercache.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component("authorizationManager")
public class AuthorizationManager {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationManager.class);

    private final EntityIdentifier entityIdentifier;
    private final OwnershipCache ownershipCache;
    private final UserCache userCache;
    private final UserRegistrationService userRegistrationService;
    private final UserService userService;

    public AuthorizationManager(
            EntityIdentifier entityIdentifier,
            OwnershipCache ownershipCache,
            UserCache userCache,
            UserRegistrationService userRegistrationService,
            UserService userService) {
        this.entityIdentifier = entityIdentifier;
        this.ownershipCache = ownershipCache;
        this.userCache = userCache;
        this.userRegistrationService = userRegistrationService;
        this.userService = userService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReadyEvent() {
        ownershipCache.loadOwnerships();
    }

    public void addOwnership(SecuredEntity<?> entity) {
        var securedEntityType = entityIdentifier.getSecuredEntityType(entity);
        ownershipCache.addOwnership(securedEntityType, entity.getId().longValue(), getCurrentUser().id());
    }

    public void addOwnership(SecuredEntityType entityType, Long entityId, Long ownerId) {
        ownershipCache.addOwnership(entityType, entityId, ownerId);
    }

    public Boolean currentUserCanDelete(SecuredEntity<?> entity) {
        var securedEntityType = entityIdentifier.getSecuredEntityType(entity);
        return doesCurrentUserOwnEntity(securedEntityType, entity.getId().longValue());
    }

    public Boolean currentUserCanDelete(SecuredEntityType securedEntityType, Number entityId) {
        return doesCurrentUserOwnEntity(securedEntityType, entityId.longValue());
    }

    public Boolean currentUserCanDelete(String sourceKey) {
        var deletePermission = String.format("source:%s:delete", sourceKey);
        return currentUserHasPermission(deletePermission);
    }

    public Boolean currentUserCanRead(SecuredEntity<?> entity) {
        var securedEntityType = entityIdentifier.getSecuredEntityType(entity);
        return doesCurrentUserOwnEntity(securedEntityType, entity.getId().longValue());
    }

    public Boolean currentUserCanRead(SecuredEntityType securedEntityType, Number entityId) {
        return doesCurrentUserOwnEntity(securedEntityType, entityId.longValue());
    }

    public Boolean currentUserCanRead(String sourceKey) {
        var readPermission = String.format("source:%s:read", sourceKey);
        return currentUserHasPermission(readPermission);
    }

    public Boolean currentUserCanUpdate(SecuredEntity<?> entity) {
        var securedEntityType = entityIdentifier.getSecuredEntityType(entity);
        return doesCurrentUserOwnEntity(securedEntityType, entity.getId().longValue());
    }

    public Boolean currentUserCanUpdate(SecuredEntityType securedEntityType, Number entityId) {
        return doesCurrentUserOwnEntity(securedEntityType, entityId.longValue());
    }

    public Boolean currentUserCanUpdate(String sourceKey) {
        var updatePermission = String.format("source:%s:update", sourceKey);
        return currentUserHasPermission(updatePermission);
    }

    public Boolean currentUserHasPermission(String permission) {
        var userProfile = getCurrentUserProfile();
        return userProfile.getPermissions().containsKey(permission);
    }

    public Boolean doesCurrentUserOwnEntity(SecuredEntityType securedEntityType, Integer entityId) {
        var longEntityId = entityId.longValue();
        var userProfile = getCurrentUserProfile();
        var ownerId = ownershipCache.getOwnerId(securedEntityType, longEntityId);
        return ownerId.equals(userProfile.getUserId());
    }

    public Boolean doesCurrentUserOwnEntity(SecuredEntityType securedEntityType, Long entityId) {
        var userProfile = getCurrentUserProfile();
        var ownerId = ownershipCache.getOwnerId(securedEntityType, entityId);
        return ownerId.equals(userProfile.getUserId());
    }

    public void fillDeleteAccess(SecuredEntity<?> entity, SecuredEntityDTO entityDTO) {
        // If the current user didn't create the entity, then set no delete access
        var securedEntityType = entityIdentifier.getSecuredEntityType(entity);
        if (!doesCurrentUserOwnEntity(securedEntityType, entity.getId().longValue())) {
            entityDTO.setHasDeleteAccess(false);
            return;
        }

        // Current user created the entity, determine if they have delete permission
        var canDelete = false;
        var resourceName = entityIdentifier.getEntityName(securedEntityType);
        canDelete = currentUserHasPermission(resourceName + ":delete");
        entityDTO.setHasDeleteAccess(canDelete);
    }

    public void fillPermissions(SecuredEntity<?> entity, SecuredEntityDTO entityDTO) {
        fillWriteAccess(entity, entityDTO);
        fillDeleteAccess(entity, entityDTO);
    }

    public void fillWriteAccess(SecuredEntity<?> entity, SecuredEntityDTO entityDTO) {
        // If the current user didn't create the entity, then set no write access
        var securedEntityType = entityIdentifier.getSecuredEntityType(entity);
        if (!doesCurrentUserOwnEntity(securedEntityType, entity.getId().longValue())) {
            entityDTO.setHasWriteAccess(false);
            return;
        }

        // Current user created the entity, determine if they have update permission
        var canEdit = false;
        var resourceName = entityIdentifier.getEntityName(securedEntityType);
        canEdit = currentUserHasPermission(resourceName + ":update");
        entityDTO.setHasWriteAccess(canEdit);
    }

    public Date getCurrentUserLastViewedNotificationTime() {
        var userProfile = getCurrentUserProfile();
        return userProfile.getLastViewedNotificationsTime();
    }

    public Long getOwnerId(SecuredEntityType entityType, Long entityId) {
        return ownershipCache.getOwnerId(entityType, entityId);
    }

    public UserDTO getUserByLogin(String login) {
        var userProfile = userCache.getUserProfile(login);
        return new UserDTO(
                userProfile.getUserId(),
                userProfile.getName(),
                userProfile.getLogin());
    }

    public UserDTO getCurrentUser() {
        var userProfile = getCurrentUserProfile();
        return new UserDTO(
                userProfile.getUserId(),
                userProfile.getName(),
                userProfile.getLogin());
    }

    public List<String> getRawPermissions(String login) {
        var userProfile = userCache.getUserProfile(login);

        var rawPermissions = new ArrayList<String>();
        for (var permission : userProfile.getPermissions().values()) {
            rawPermissions.add(permission.permission());
        }
        return rawPermissions;
    }

    public UserDTO registerNewUser(String login, String name) {
        try {
            var user = userRegistrationService.registerNewUser(login, name, UserOrigin.SYSTEM);
            return UserEntityToUserDTOConverter.convert(user);

        } catch (Exception e) {
            log.error("Error registering new user [" + login + "]", e);
            throw new RuntimeException("Failed to register new user");
        }
    }

    public void removeOwnership(SecuredEntityType entityType, Integer entityId) {
        removeOwnership(entityType, entityId.longValue());
    }

    public void removeOwnership(SecuredEntityType entityType, Long entityId) {
        ownershipCache.removeOwnership(entityType, entityId);
    }

    @Transactional
    public void setCurrentUserLastViewedNotificationTime(Date lastViewedAt) {
        // This will persist the last viewed notification time and will also update
        // the user cache
        var userProfile = getCurrentUserProfile();
        userService.setUserLastViewedNotificationTime(userProfile.getUserId(), lastViewedAt);
    }

    public void throwIfCurrentUserDoesntOwn(SecuredEntityType securedEntityType, Integer entityId) {
        if (!doesCurrentUserOwnEntity(SecuredEntityType.TAG, entityId)) {
            var entityName = entityIdentifier.getEntityName(securedEntityType);
            throw new AccessDeniedException(String.format("You do not have access to %s (id: %d)", entityName, entityId));
        }
    }

    public void throwIfCurrentUserDoesntOwn(SecuredEntityType securedEntityType, Long entityId) {
        if (!doesCurrentUserOwnEntity(SecuredEntityType.TAG, entityId)) {
            var entityName = entityIdentifier.getEntityName(securedEntityType);
            throw new AccessDeniedException(String.format("You do not have access to %s (id: %d)", entityName, entityId));
        }
    }

    public void throwIfCurrentUserDoesntOwn(SecuredEntity<?> entity) {
        var securedEntityType = entityIdentifier.getSecuredEntityType(entity);
        var entityId = entity.getId().longValue();
        if (!doesCurrentUserOwnEntity(securedEntityType, entityId)) {
            var entityName = entityIdentifier.getEntityName(securedEntityType);
            throw new AccessDeniedException(String.format("You do not have access to %s (id: %d)", entityName, entityId));
        }
    }

    public Boolean userExists(String login) {
        var idLoginBiMap = userCache.getIdLoginBiMap();
        var userId = idLoginBiMap.inverse().get(login);
        return userId != null;
    }

    private UserProfile getCurrentUserProfile() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Cannot find the Security Context");
        }

        String username;
        if (authentication.getPrincipal() instanceof String &&
                authentication.getPrincipal().equals(AnonymousUser.NAME)) {
            username = AnonymousUser.NAME;
        } else if (authentication.getPrincipal() instanceof UserDetails currentUserDetails) {
            username = currentUserDetails.getUsername();
        } else if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            username = oidcUser.getEmail();
        } else {
            username = null;
        }

        if (username == null) {
            throw new RuntimeException("Cannot find the current user");
        }

        return userCache.getUserProfile(username);
    }

    public Boolean canReadSensitiveInfo() {
        return currentUserHasPermission(Permissions.READ_SENSITIVE_INFO);
    }

}
