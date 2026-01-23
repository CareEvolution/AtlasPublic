package org.ohdsi.webapi.authorization.event;

import org.springframework.context.ApplicationEvent;

public abstract class SecurityEvent extends ApplicationEvent {
    protected SecurityEvent(Object source) {
        super(source);
    }
}
