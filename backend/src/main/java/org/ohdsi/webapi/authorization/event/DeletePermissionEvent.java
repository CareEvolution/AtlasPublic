package org.ohdsi.webapi.authorization.event;

public class DeletePermissionEvent extends SecurityEvent {
    private final long permissionId;
    private final long roleId;

    public DeletePermissionEvent(Object source, long permissionId, long roleId) {
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
