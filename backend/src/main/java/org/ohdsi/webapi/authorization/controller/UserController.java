package org.ohdsi.webapi.authorization.controller;

import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.dto.*;
import org.ohdsi.webapi.authorization.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API for managing users. This controller provides endpoints for retrieving user information, 
 * user permissions, and user roles.
 */
@RestController
@RequestMapping("/user")
public class UserController {
    private final AuthorizationManager authorizationManager;
    private final UserService userService;

    public UserController(
            AuthorizationManager authorizationManager,
            UserService userService) {
        this.authorizationManager = authorizationManager;
        this.userService = userService;
    }

    /**
     * Get information about the current user.
     * @return The current user's information
     */
    @GetMapping("/me")
    public UserMeDTO getMe() {
        var currentUser = authorizationManager.getCurrentUser();
        if (currentUser == null) {
            throw new AuthenticationCredentialsNotFoundException("Login required");
        }
        return userService.getUserMe(currentUser.id());
    }

    /**
     * Get the list of all users in the system.
     * @return A list of all users
     */
    @GetMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_USER)")
    public List<UserDTO> getUsers() {
        return userService.getUsers();
    }

    /**
     * Get permissions assigned to a user.
     * @param userId
     *    The user identifier
     * @return
     *   The list of permissions assigned to the user
     */
    @GetMapping("/{userId}/permissions")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_USER) and @authorizationManager.currentUserHasPermission(@PERMS.READ_PERMISSION)")
    public List<PermissionDTO> getUserPermissions(@PathVariable("userId") Long userId) {
        return userService.getUserPermissions(userId);
    }

    /**
     * Get roles assigned to a user.
     * @param userId
     *    The user identifier
     * @return
     *   The list of roles assigned to the user
     */
    @GetMapping("/{userId}/roles")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_USER) and @authorizationManager.currentUserHasPermission(@PERMS.READ_ROLE)")
    public List<RoleDTO> getUserRoles(@PathVariable("userId") Long userId) {
        return userService.getUserRoles(userId);
    }
}
