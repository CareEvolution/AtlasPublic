package org.ohdsi.webapi.audittrail.listeners;

import org.ohdsi.webapi.audittrail.AuditTrailService;
import org.ohdsi.webapi.audittrail.events.AuditTrailSessionCreatedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class AuditTrailSuccessLoginListener implements ApplicationListener<AuditTrailSessionCreatedEvent> {
    private final AuditTrailService auditTrailService;

    public AuditTrailSuccessLoginListener(@Lazy AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    @Override
    public void onApplicationEvent(final AuditTrailSessionCreatedEvent event) {
        auditTrailService.logSuccessfulLogin(event.getLogin(), event.getSessionId(), event.getRemoteHost());
    }
}
