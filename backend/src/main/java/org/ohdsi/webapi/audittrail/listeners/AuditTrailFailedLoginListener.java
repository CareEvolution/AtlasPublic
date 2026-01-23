package org.ohdsi.webapi.audittrail.listeners;

import org.ohdsi.webapi.audittrail.AuditTrailService;
import org.ohdsi.webapi.audittrail.events.AuditTrailLoginFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class AuditTrailFailedLoginListener implements ApplicationListener<AuditTrailLoginFailedEvent> {
    private final AuditTrailService auditTrailService;

    public AuditTrailFailedLoginListener(@Lazy AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    @Override
    public void onApplicationEvent(final AuditTrailLoginFailedEvent event) {
        auditTrailService.logFailedLogin(event.getLogin(), event.getRemoteHost());
    }
}
