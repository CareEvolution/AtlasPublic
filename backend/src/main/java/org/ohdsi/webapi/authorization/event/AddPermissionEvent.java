package org.ohdsi.webapi.authorization.event;

public class AddPermissionEvent extends SecurityEvent {
    private final long permissionId;
    private final long roleId;

    public AddPermissionEvent(Object source, long permissionId, long roleId) {
        super(source);
        this.permissionId = permissionId;
        this.roleId = roleId;
    }

    public long getPermissionId() {
        return permissionId;
    }

    public long getRoleId() {
        return roleId;
    }
}
