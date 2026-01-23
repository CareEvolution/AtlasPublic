package org.ohdsi.webapi.authorization.event;

public class FailedLogoutEvent extends SecurityEvent {
    private final String login;

    public FailedLogoutEvent(Object source, String login) {
        super(source);
        this.login = login;
    }

    public String getLogin() {
        return login;
    }
}
