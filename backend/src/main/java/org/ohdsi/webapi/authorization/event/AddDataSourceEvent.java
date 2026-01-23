package org.ohdsi.webapi.authorization.event;

public class AddDataSourceEvent extends SecurityEvent {
    private final int id;
    private final String name;

    public AddDataSourceEvent(Object source, int id, String name) {
        super(source);
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
