package org.ohdsi.webapi.authorization.event;

public class DeleteRoleEvent extends SecurityEvent {
    private final long id;

    public DeleteRoleEvent(Object source, long id) {
        super(source);
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
