package org.ohdsi.webapi.authorization.event;

public class SuccessLogoutEvent extends SecurityEvent {
    private final String login;

    public SuccessLogoutEvent(Object source, String login) {
        super(source);
        this.login = login;
    }

    public String getLogin() {
        return login;
    }
}
