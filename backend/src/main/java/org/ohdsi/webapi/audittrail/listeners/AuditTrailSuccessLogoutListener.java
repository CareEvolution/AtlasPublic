package org.ohdsi.webapi.audittrail.listeners;

import org.ohdsi.webapi.audittrail.AuditTrailService;
import org.ohdsi.webapi.authorization.event.SuccessLogoutEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class AuditTrailSuccessLogoutListener implements ApplicationListener<SuccessLogoutEvent> {
    private final AuditTrailService auditTrailService;

    public AuditTrailSuccessLogoutListener(@Lazy AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    @Override
    public void onApplicationEvent(final SuccessLogoutEvent event) {
        auditTrailService.logSuccessfulLogout(event.getLogin());
    }
}
