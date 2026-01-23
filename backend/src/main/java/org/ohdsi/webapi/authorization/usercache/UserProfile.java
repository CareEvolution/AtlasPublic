package org.ohdsi.webapi.authorization.usercache;

import org.ohdsi.webapi.authorization.dto.PermissionDTO;
import org.ohdsi.webapi.authorization.dto.RoleDTO;
import org.ohdsi.webapi.authorization.model.UserOrigin;

import java.util.Date;
import java.util.Map;

public class UserProfile {
    private Long userId;
    private String login;
    private String name;
    private Date lastViewedNotificationsTime;
    private UserOrigin origin;
    private Map<String, RoleDTO> roles;
    private Map<String, PermissionDTO> permissions;

    public UserProfile(
            Long userId,
            String login,
            String name,
            Date lastViewedNotificationsTime,
            UserOrigin origin,
            Map<String, RoleDTO> roles,
            Map<String, PermissionDTO> permissions) {
        this.userId = userId;
        this.login = login;
        this.name = name;
        this.lastViewedNotificationsTime = lastViewedNotificationsTime;
        this.origin = origin;
        this.roles = roles;
        this.permissions = permissions;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getLastViewedNotificationsTime() {
        return lastViewedNotificationsTime;
    }

    public void setLastViewedNotificationsTime(Date lastViewedNotificationsTime) {
        this.lastViewedNotificationsTime = lastViewedNotificationsTime;
    }

    public UserOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(UserOrigin origin) {
        this.origin = origin;
    }

    public Map<String, RoleDTO> getRoles() {
        return roles;
    }

    public void setRoles(Map<String, RoleDTO> roles) {
        this.roles = roles;
    }

    public Map<String, PermissionDTO> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, PermissionDTO> permissions) {
        this.permissions = permissions;
    }
}
