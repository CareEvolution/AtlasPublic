package org.ohdsi.webapi.audittrail.listeners;

import org.ohdsi.webapi.audittrail.AuditTrailService;
import org.ohdsi.webapi.authorization.event.FailedLogoutEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class AuditTrailFailedLogoutListener implements ApplicationListener<FailedLogoutEvent> {
    private final AuditTrailService auditTrailService;

    public AuditTrailFailedLogoutListener(@Lazy AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    @Override
    public void onApplicationEvent(final FailedLogoutEvent event) {
        auditTrailService.logFailedLogout(event.getLogin());
    }
}
