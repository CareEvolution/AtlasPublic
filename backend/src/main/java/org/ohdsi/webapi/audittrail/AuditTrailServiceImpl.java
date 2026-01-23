package org.ohdsi.webapi.audittrail;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.ohdsi.webapi.Constants;
import org.ohdsi.webapi.cohortsample.dto.CohortSampleDTO;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import jakarta.ws.rs.core.Response;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
class AuditTrailServiceImpl implements AuditTrailService {
    private final Logger AUDIT_LOGGER = LogManager.getLogger("audit");
    private final Logger AUDIT_EXTRA_LOGGER = LogManager.getLogger("audit-extra");

    private static final int MAX_ENTRY_LENGTH = 2048;
    private static final String EXTRA_LOG_REFERENCE_MESSAGE = "... Log entry exceeds 2048 chars length. Please see the whole message in extra log file, entry id = %s";

    private static final String USER_LOGIN_SUCCESS_MESSAGE = "User successfully logged in";
    private static final String USER_LOGIN_FAILURE_MESSAGE = "User login failed";
    private static final String USER_LOGOUT_SUCCESS_MESSAGE = "User successfully logged out";
    private static final String USER_LOGOUT_FAILURE_MESSAGE = "User logout failed";

    private static final String JOB_STARTED_TEMPLATE = "Job execution started";
    private static final String JOB_COMPLETED_TEMPLATE = "Job execution completed successfully";
    private static final String JOB_FAILED_TEMPLATE = "Job execution failed";

    private static final String LIST_OF_TEMPLATE = "list of %s objects %s";
    private static final String MAP_OF_TEMPLATE = "map (key: %s) of %s objects %s";
    private static final String FILE_TEMPLATE = "file %s (%s bytes)";
    private static final String PATIENT_IDS_TEMPLATE = " Patient IDs (%s): ";

    private static final String FAILURE = "FAILURE (see application log for details)";
    private static final String ANONYMOUS = "anonymous";
    private static final String NO_SESSION = "NO_SESSION";
    private static final String NO_LOCATION = "NO_LOCATION";
    private static final String EMPTY_LIST = "empty list";
    private static final String EMPTY_MAP = "empty map";

    private final AtomicInteger extraLogIdSuffix = new AtomicInteger();

    @Override
    public void logSuccessfulLogin(final String login, final String sessionId, final String remoteHost) {
        ThreadContext.put("_user", login);
        ThreadContext.put("_sessionId", sessionId);
        ThreadContext.put("_remoteHost", remoteHost);
        log(USER_LOGIN_SUCCESS_MESSAGE);
    }

    @Override
    public void logFailedLogin(final String login, final String remoteHost) {
        ThreadContext.put("_user", login);
        ThreadContext.put("_remoteHost", remoteHost);
        log(USER_LOGIN_FAILURE_MESSAGE);
    }

    @Override
    public void logSuccessfulLogout(final String login) {
        ThreadContext.put("_user", login);
        log(USER_LOGOUT_SUCCESS_MESSAGE);
    }

    @Override
    public void logFailedLogout(final String login) {
        ThreadContext.put("_user", login);
        log(USER_LOGOUT_FAILURE_MESSAGE);
    }

    @Override
    public void logRestCall(final AuditTrailEntry entry, final boolean success) {
        ThreadContext.put("_user", getCurrentUserField(entry));
        ThreadContext.put("_remoteHost", entry.getRemoteHost());
        ThreadContext.put("_sessionId", getSessionIdField(entry));
        ThreadContext.put("_actionLocation", getActionLocationField(entry));
        ThreadContext.put("_restCall", getRestCallField(entry));

        if (success) {
            final String additionalInfo = getAdditionalInfo(entry);
            if (!StringUtils.isBlank(additionalInfo)) {
                ThreadContext.put("_additionalInfo", additionalInfo);
            }
            log("REST call successful");
        } else {
            log(FAILURE);
        }
    }

    @Override
    public void logJobStart(final JobExecution jobExecution) {
        logJob(jobExecution, JOB_STARTED_TEMPLATE);
    }

    @Override
    public void logJobCompleted(final JobExecution jobExecution) {
        logJob(jobExecution, JOB_COMPLETED_TEMPLATE);
    }

    @Override
    public void logJobFailed(JobExecution jobExecution) {
        logJob(jobExecution, JOB_FAILED_TEMPLATE);
    }

    private void log(String message) {
        if (message.length() > MAX_ENTRY_LENGTH) {
            final String extraLogId = String.valueOf(extraLogIdSuffix.incrementAndGet());
            AUDIT_EXTRA_LOGGER.info(String.format(EXTRA_LOG_REFERENCE_MESSAGE, extraLogId));
            message = message.substring(0, MAX_ENTRY_LENGTH - extraLogId.length()) + extraLogId;
        }

        AUDIT_LOGGER.info(message);
        ThreadContext.clearAll();
    }

    private String getSessionIdField(final AuditTrailEntry entry) {
        return entry.getSessionId() != null ? entry.getSessionId() : NO_SESSION;
    }

    private String getCurrentUserField(final AuditTrailEntry entry) {
        return (entry.getCurrentUser() != null ? String.valueOf(entry.getCurrentUser()) : ANONYMOUS);
    }

    private String getActionLocationField(final AuditTrailEntry entry) {
        return entry.getActionLocation() != null ? entry.getActionLocation() : NO_LOCATION;
    }

    private String getRestCallField(final AuditTrailEntry entry) {
        final StringBuilder sb = new StringBuilder();
        sb.append(entry.getRequestMethod())
                .append(" ")
                .append(entry.getRequestUri());
        if (!StringUtils.isBlank(entry.getQueryString())) {
            sb.append("?").append(entry.getQueryString());
        }
        return sb.toString();
    }

    private String getAdditionalInfo(final AuditTrailEntry entry) {
        final StringBuilder additionalInfo = new StringBuilder();

        final String returnedObjectFields = getReturnedObjectFields(entry.getReturnedObject());
        if (!StringUtils.isBlank(returnedObjectFields)) {
            additionalInfo.append(returnedObjectFields);
        }

        // File entry log
        if (entry.getReturnedObject() instanceof Response) {
            try {
                final Object entity = ((Response) entry.getReturnedObject()).getEntity();
                if (entity instanceof File) {
                    final File file = (File) entity;
                    return String.format(FILE_TEMPLATE, file.getName(), file.length());
                }
                return null;
            } catch (final Exception e) {
                return null;
            }
        }

        // Patient IDs log
        if (entry.getReturnedObject() instanceof CohortSampleDTO) {
            final CohortSampleDTO sampleDto = (CohortSampleDTO) entry.getReturnedObject();
            if (sampleDto.getElements().isEmpty()) {
                additionalInfo.append(String.format(PATIENT_IDS_TEMPLATE, 0)).append("none");
            } else {
                additionalInfo.append(String.format(PATIENT_IDS_TEMPLATE, sampleDto.getElements().size()));
                sampleDto.getElements().forEach((e) -> {
                    additionalInfo.append(e.getPersonId()).append(" ");
                });
            }
        }

        return additionalInfo.toString();
    }

    private String getReturnedObjectFields(final Object returnedObject) {
        if (returnedObject == null) {
            return null;
        }

        if (returnedObject instanceof Collection) {
            final Collection<?> c = (Collection<?>) returnedObject;
            if (!c.isEmpty()) {
                final String fields = collectClassFieldNames(c.iterator().next().getClass());
                return fields != null ? String.format(LIST_OF_TEMPLATE, c.size(), fields) : null;
            }
            return EMPTY_LIST;
        }
        if (returnedObject instanceof Map) {
            final Map<?, ?> map = (Map<?, ?>) returnedObject;
            if (!map.isEmpty()) {
                final Map.Entry<?, ?> entry = map.entrySet().iterator().next();
                final Class<?> keyClass = entry.getKey().getClass();
                final Class<?> valueClass = entry.getValue().getClass();
                final String valueFields = collectClassFieldNames(valueClass);
                return valueFields != null
                        ? String.format(MAP_OF_TEMPLATE, keyClass.getSimpleName(), map.size(), valueFields)
                        : null;
            }
            return EMPTY_MAP;
        } else {
            return collectClassFieldNames(returnedObject.getClass());
        }
    }

    private String collectClassFieldNames(final Class<?> klass) {
        if (!klass.getPackage().getName().startsWith("org.ohdsi.")) {
            return null;
        }

        // collect only first level field names
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        ReflectionUtils.doWithFields(klass,
                field -> sb.append(field.getName()).append("::").append(field.getType().getSimpleName()).append(","));
        if (sb.length() > 1) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append("}");
        return sb.toString();
    }

    private void logJob(final JobExecution jobExecution, final String jobMessage) {
        final JobParameters jobParameters = jobExecution.getJobParameters();
        final String author = jobParameters.getString(Constants.Params.JOB_AUTHOR);
        if (author.equals("anonymous")) { // system jobs
            return;
        }

        ThreadContext.put("_author", author);
        ThreadContext.put("_jobId", jobExecution.getJobId().toString());
        ThreadContext.put("_jobName", jobParameters.getString(Constants.Params.JOB_NAME));
        log(jobMessage);
    }
}
