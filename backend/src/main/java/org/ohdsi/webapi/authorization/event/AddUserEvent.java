package org.ohdsi.webapi.authorization.event;

public class AddUserEvent extends SecurityEvent {
    private final long id;
    private final String login;

    public AddUserEvent(Object source, long id, String login) {
        super(source);
        this.id = id;
        this.login = login;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }
}
