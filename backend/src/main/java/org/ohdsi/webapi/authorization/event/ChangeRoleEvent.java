package org.ohdsi.webapi.authorization.event;

public class ChangeRoleEvent extends SecurityEvent {
    private final long id;
    private final String name;

    public ChangeRoleEvent(Object source, long id, String name) {
        super(source);
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
