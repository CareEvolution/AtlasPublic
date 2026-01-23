package org.ohdsi.webapi.authorization.controller;

import org.ohdsi.webapi.authorization.EntityIdentifier;
import org.ohdsi.webapi.authorization.model.AccessType;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.dto.AccessRequestDTO;
import org.ohdsi.webapi.authorization.dto.PermissionDTO;
import org.ohdsi.webapi.authorization.dto.RoleDTO;
import org.ohdsi.webapi.authorization.model.SecuredEntityType;
import org.ohdsi.webapi.authorization.service.PermissionService;
import org.ohdsi.webapi.authorization.service.RoleService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Services related to working with security permissions
 *
 */
@RestController
@RequestMapping("/permission")
@Transactional
public class PermissionController {
    private final AuthorizationManager authorizationManager;
    private final EntityIdentifier entityIdentifier;
    private final RoleService roleService;
    private final PermissionService permissionService;

    public PermissionController(
            AuthorizationManager authorizationManager,
            EntityIdentifier entityIdentifier,
            RoleService roleService,
            PermissionService permissionService) {
        this.authorizationManager = authorizationManager;
        this.entityIdentifier = entityIdentifier;
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    /**
     * Get the list of all permissions
     *
     * @return A list of permissions
     */
    @GetMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_PERMISSION)")
    public List<PermissionDTO> getAllPermissions() {
        return permissionService.getAllPermissions();
    }

    /**
     * Get the roles that contain (caseless) a search target.
     *
     * @param searchTarget
     *         The value to use for the caseless contains operation
     * @return A list of roles containing the search target
     */
    @GetMapping("/access/suggest")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_ROLE)")
    public List<RoleDTO> getRolesThatContainSearchTarget(@RequestParam(name="roleSearch") String searchTarget) {
        return roleService.getRolesThatContainSearchTarget(searchTarget);
    }

    /**
     * Get roles that have a permission type (READ/WRITE) to entity
     *
     * @param securedEntityType
     *         The entity type
     * @param entityId
     *         The entity ID
     * @return The list of roles that have either read or write permissions for the entity
     *
     */
    @GetMapping("/access/{securedEntityType}/{entityId}/{permType}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_ROLE) and @authorizationManager.currentUserHasPermission(@PERMS.READ_PERMISSION)")
    public List<RoleDTO> getRolesWithEntityPermissions(
            @PathVariable SecuredEntityType securedEntityType,
            @PathVariable Long entityId,
            @PathVariable AccessType permType) {

        // From the old codebase: the user is expected to own the specified entity.
        var entityName = entityIdentifier.getEntityName(securedEntityType);
        if (!authorizationManager.doesCurrentUserOwnEntity(securedEntityType, entityId)) {
            throw new AccessDeniedException(
                    String.format("You are not the owner of entity %s (id=%d)", entityName, entityId));
        }

        return roleService.findAllRolesWithNamedEntityReadOrWritePermissions(permType, entityName);
    }

    /**
     * Get roles that have a permission type (READ/WRITE) to entity
     *
     * @param securedEntityType
     *         The entity type
     * @param entityId
     *         The entity ID
     * @return The list of roles for the entity that have WRITE permissions.
     */
    @GetMapping("/access/{securedEntityType}/{entityId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_ROLE) and @authorizationManager.currentUserHasPermission(@PERMS.READ_PERMISSION)")
    public List<RoleDTO> listAccessesForEntity(
            @PathVariable SecuredEntityType securedEntityType,
            @PathVariable Long entityId) {

        // From the old codebase: the user is expected to own the specified entity.
        var entityName = entityIdentifier.getEntityName(securedEntityType);
        if (!authorizationManager.doesCurrentUserOwnEntity(securedEntityType, entityId)) {
            throw new AccessDeniedException(
                    String.format("You are not the owner of entity %s (id=%d)", entityName, entityId));
        }

        return roleService.findAllRolesWithNamedEntityReadOrWritePermissions(AccessType.WRITE, entityName);
    }

    /**
     * Grant group of permissions (READ / WRITE / ...) for the specified entity to the given role.
     * Only owner of the entity can do that.
     *
     * @param securedEntityType
     *         The entity type
     * @param entityId
     *         The entity ID
     * @param roleId
     *         The role ID
     * @param accessRequestDTO
     *         The access request object
     */
    @PostMapping("/access/{securedEntityType}/{entityId}/role/{roleId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.FORBIDDEN)")
    public void grantEntityPermissionsForRole(
            @PathVariable SecuredEntityType securedEntityType,
            @PathVariable Long entityId,
            @PathVariable Long roleId,
            @RequestBody AccessRequestDTO accessRequestDTO) {
        // TODO post-MVP: we do not have a permission "grant" system.
        //                for now, deny access
    }

    /**
     * Remove group of permissions for the specified entity to the given role.
     *
     * @param securedEntityType
     *         The entity type
     * @param entityId
     *         The entity ID
     * @param roleId
     *         The role ID
     * @param accessRequestDTO
     *         The access request object
     */
    @DeleteMapping("/access/{securedEntityType}/{entityId}/role/{roleId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.FORBIDDEN)")
    public void revokeEntityPermissionsFromRole(
            @PathVariable SecuredEntityType securedEntityType,
            @PathVariable Long entityId,
            @PathVariable Long roleId,
            @RequestBody AccessRequestDTO accessRequestDTO){
        // TODO post-MVP: we do not have a permission "grant" system.
        //                for now, deny access
    }
}
