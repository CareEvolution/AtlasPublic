package org.ohdsi.webapi.audittrail;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.ohdsi.webapi.Constants;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
@Order(0)
@ConditionalOnProperty(prefix= "webapi.audit-trail", name = "enabled", havingValue = "true")
public class AuditTrailAspect {

    private final AuditTrailService auditTrailService;
    private final AuthorizationManager authorizationManager;

    public AuditTrailAspect(AuditTrailService auditTrailService, AuthorizationManager authorizationManager) {
        this.auditTrailService = auditTrailService;
        this.authorizationManager = authorizationManager;
    }

    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public void restGetPointcut() {
    }

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)")
    public void restPostPointcut() {
    }

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PutMapping)")
    public void restPutPointcut() {
    }

    @Pointcut("@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void restDeletePointcut() {
    }

    // This is so we can exclude calls to this controller
    @Pointcut("execution(public * org.ohdsi.webapi.job.NotificationController.*(..))")
    public void notificationsPointcut() {
    }

    // This is so we can exclude calls to the vocabulary service getInfo method
    @Pointcut("execution(public * org.ohdsi.webapi.vocabulary.VocabularyService.getInfo(..))")
    public void vocabularyServiceGetInfoPointcut() {
    }

    @Around(
        "(restGetPointcut() || restPostPointcut() || restPutPointcut() || restDeletePointcut())"
            // exclude system calls
            + " && !notificationsPointcut()"
            + " && !vocabularyServiceGetInfoPointcut()"
    )
    public Object auditLog(ProceedingJoinPoint joinPoint) throws Throwable {
        final HttpServletRequest request = getHttpServletRequest();

        if (request == null) { // system call
            return joinPoint.proceed();
        }

        final var entry = new AuditTrailEntry();
        entry.setRemoteHost(request.getRemoteHost());

        var currentUser = authorizationManager.getCurrentUser();
        if (currentUser != null) {
            entry.setCurrentUser(currentUser.login());
        }

        var cookies = request.getCookies();
        if (cookies != null) {
            var sessionCookie = Arrays.stream(cookies)
                    .filter(c -> c.getName().equals("JSESSIONID")).findFirst();
            sessionCookie.ifPresent(cookie -> entry.setSessionId(cookie.getValue()));
        }

        entry.setActionLocation(request.getHeader(Constants.Headers.ACTION_LOCATION));
        entry.setRequestMethod(request.getMethod());
        entry.setRequestUri(request.getRequestURI());
        entry.setQueryString(request.getQueryString());

        try {
            final var returnedObject = joinPoint.proceed();
            entry.setReturnedObject(returnedObject);

            auditTrailService.logRestCall(entry, true);

            return returnedObject;
        } catch (final Throwable t) {
            auditTrailService.logRestCall(entry, false);
            throw t;
        }

    }

    private HttpServletRequest getHttpServletRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (final Exception e) {
            return null;
        }
    }
}