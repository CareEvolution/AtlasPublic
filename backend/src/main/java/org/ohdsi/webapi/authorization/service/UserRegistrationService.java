package org.ohdsi.webapi.authorization.service;

import org.ohdsi.webapi.authorization.entity.UserEntity;
import org.ohdsi.webapi.authorization.model.UserOrigin;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

@Service
public class UserRegistrationService {
    private final UserService userService;
    private final RoleService roleService;
    private final WebApiProperties webApiProperties;

    public UserRegistrationService(
            RoleService roleService,
            UserService userService,
            WebApiProperties webApiProperties) {
        this.roleService = roleService;
        this.userService = userService;
        this.webApiProperties = webApiProperties;
    }

    @Transactional
    public UserEntity registerNewUser(final String login, final String name, UserOrigin userOrigin) {
        if (!StringUtils.hasText(login)) {
            throw new IllegalArgumentException("Login must be provided to register a new user");
        }

        try {
            var existingUser = userService.getUser(login);
            if (existingUser != null) {
                throw new IllegalArgumentException(String.format("User with login '%s' already exists", login));
            }
        } catch (NotFoundException ex) {
            // User does not exist, continue with registration
        }

        var user = userService.createUser(login, name, userOrigin);

        var defaultRoles = webApiProperties.user().getDefaultRoles();
        if (!defaultRoles.isEmpty()) {
            for (var roleName : defaultRoles) {
                var role = roleService.getRole(roleName, true);
                roleService.addRoleToUser(user, role);
            }
        }

        return user;
    }
}
