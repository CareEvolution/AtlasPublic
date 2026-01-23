package org.ohdsi.webapi.authorization.controller;

import org.ohdsi.webapi.authorization.dto.PermissionDTO;
import org.ohdsi.webapi.authorization.dto.RoleDTO;
import org.ohdsi.webapi.authorization.dto.UserDTO;
import org.ohdsi.webapi.authorization.service.RoleService;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for working with security roles
 *
 */
@RestController
@RequestMapping("/role")
public class RoleController {
    private final RoleService roleService;
    private final ConversionService conversionService;

    public RoleController(RoleService roleService, ConversionService conversionService) {
        this.roleService = roleService;
        this.conversionService = conversionService;
    }

    /**
     * Add permissions to a role
     * @param roleId
     *        The role identifier
     * @param plusSignSeparatedPermissionIds
     *       A string of plus sign separated permission identifiers to add to the role
     */
    @PutMapping("/{roleId}/permissions/{permissionIdList}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_ROLE) and @authorizationManager.currentUserHasPermission(@PERMS.READ_PERMISSION)")
    public void addPermissionsToRole(
            @PathVariable("roleId") Long roleId,
            @PathVariable("permissionIdList") String plusSignSeparatedPermissionIds) {
        roleService.addPermissionsToRole(roleId, plusSignSeparatedPermissionIds);
    }

    /**
     * Add users to a role
     * @param roleId
     *       The role identifier
     * @param plusSignSeparatedUserIds
     *       A string of plus sign separated user identifiers to add to the role
     */
    @PutMapping("/{roleId}/users/{userIdList}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_ROLE) and @authorizationManager.currentUserHasPermission(@PERMS.READ_USER)")
    public void addUsersToRole(
            @PathVariable("roleId") Long roleId,
            @PathVariable("userIdList") String plusSignSeparatedUserIds) {
        roleService.addUsersToRole(roleId, plusSignSeparatedUserIds);
    }

    /**
     * Update role information.
     * @param id
     *      The role identifier
     * @param role
     *     The updated role information
     * @return
     */
    @PutMapping("/{roleId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_ROLE)")
    public RoleDTO changeRoleName(
            @PathVariable("roleId") Long id,
            @RequestBody RoleDTO role) {
        return roleService.renameRole(id, role.name());
    }

    /**
     * Create a new role.
     * @param role
     *     The role information for the new role
     * @return
     */
    @PostMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.CREATE_ROLE)")
    public RoleDTO createRole(@RequestBody RoleDTO role) {
        var createdRole = roleService.createRole(role.name(), true);
        return conversionService.convert(createdRole, RoleDTO.class);
    }

    /**
     * Delete permissions from a role
     * @param roleId
     *      The role identifier
     * @param plusSignSeparatedPermissionIds
     *     A string of plus sign separated permission identifiers to delete from the role
     */
    @DeleteMapping("/{roleId}/permissions/{permissionIdList}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_ROLE)")
    public void deletePermissionsFromRole(
            @PathVariable("roleId") Long roleId,
            @PathVariable("permissionIdList") String plusSignSeparatedPermissionIds) {
        roleService.deletePermissionsFromRole(roleId, plusSignSeparatedPermissionIds);
    }

    /**
     * Delete a role
     * @param id
     *     The role identifier
     */
    @DeleteMapping("/{roleId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.DELETE_ROLE)")
    public void deleteRole(@PathVariable("roleId") Long id) {
        roleService.deleteRole(id);
    }

    /**
     * Get role information.
     * @param id
     *    The role identifier
     * @return The role information
     */
    @GetMapping("/{roleId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_ROLE)")
    public RoleDTO getRole(@PathVariable("roleId") Long id) {
        var roleEntity = roleService.getRole(id);
        return conversionService.convert(roleEntity, RoleDTO.class);
    }

    /**
     * Get permissions assigned to a role.
     * @param roleId
     *    The role identifier
     * @return
     *   The list of permissions assigned to the role
     */
    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_ROLE) and @authorizationManager.currentUserHasPermission(@PERMS.READ_PERMISSION)")
    public List<PermissionDTO> getRolePermissions(@PathVariable("roleId") Long roleId) {
        return roleService.getRolePermissions(roleId);
    }

    /**
     * Get all roles in the system.
     */
    @GetMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_ROLE)")
    public List<RoleDTO> getRoles() {
        return roleService.getRoles();
    }

    /**
     * Get users assigned to a role.
     * @param roleId
     *    The role identifier
     * @return The list of users assigned to the role
     */
    @GetMapping("/{roleId}/users")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_ROLE) and @authorizationManager.currentUserHasPermission(@PERMS.READ_USER)")
    public List<UserDTO> getRoleUsers(@PathVariable("roleId") Long roleId) {
        return roleService.getRoleUsers(roleId);
    }

    /**
     * Delete users from a role
     * @param roleId
     *    The role identifier
     * @param plusSignSeparatedUserIds
     *   A string of plus sign separated user identifiers to delete from the role
     */
    @DeleteMapping("/{roleId}/users/{userIdList}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_ROLE) and @authorizationManager.currentUserHasPermission(@PERMS.UPDATE_USER)")
    public void removeRoleFromUsers(
            @PathVariable("roleId") Long roleId,
            @PathVariable("userIdList") String plusSignSeparatedUserIds) {
        roleService.removeRoleFromUsers(roleId, plusSignSeparatedUserIds);
    }
}
