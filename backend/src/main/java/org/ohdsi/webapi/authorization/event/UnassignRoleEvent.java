package org.ohdsi.webapi.authorization.event;

public class UnassignRoleEvent extends SecurityEvent {
    private final long roleId;
    private final long userId;

    public UnassignRoleEvent(Object source, long roleId, long userId) {
        super(source);
        this.roleId = roleId;
        this.userId = userId;
    }

    public long getRoleId() {
        return roleId;
    }

    public long getUserId() {
        return userId;
    }
}
