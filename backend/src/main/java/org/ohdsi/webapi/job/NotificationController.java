package org.ohdsi.webapi.job;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for working with the system notifications
 *
 */
@RequestMapping("/notifications")
@RestController
@Transactional
public class NotificationController {
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService service;

    NotificationController(final NotificationService service) {
        this.service = service;
    }

    /**
     * Get the list of notifications
     *
     * @param hideStatuses
     *         Used to filter statuses - passes as a comma-delimited list
     * @param refreshJobs
     *         Boolean - when true, it will refresh the cache of notifications
     * @return
     */
    @GetMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_NOTIFICATIONS)")
    @Transactional(readOnly = true)
    public List<JobExecutionResource> list(
            @RequestParam(required = false) String hideStatuses,
            @RequestParam(defaultValue="FALSE") Boolean refreshJobs
    ) {
        List<BatchStatus> statuses = new ArrayList<>();
        if (StringUtils.isNotEmpty(hideStatuses)) {
            for (String status : hideStatuses.split(",")) {
                try {
                    statuses.add(BatchStatus.valueOf(status));
                } catch (IllegalArgumentException e) {
                    log.warn("An invalid status was included in the list of notification status to ignore");
                }
            }
        }
        List<JobExecutionInfo> executionInfos;
        if (refreshJobs) {
            executionInfos = service.findRefreshCacheLastJobs();
        } else {
            executionInfos = service.findLastJobs(statuses);
        }
        return executionInfos.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Gets the date when notifications were last viewed
     *
     * @return The date when notifications were last viewed
     */
    @GetMapping("/viewed")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_NOTIFICATIONS)")
    @Transactional(readOnly = true)
    public Date getLastViewedTime() {
        try {
            return service.getLastViewedTime();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the date when notifications were last viewed
     *
     * @param stamp
     *        The date when notifications were last viewed
     */
    @PostMapping("/viewed")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_NOTIFICATIONS)")
    public void setLastViewedTime(Date stamp) {
        try {
            service.setLastViewedTime(stamp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JobExecutionResource toDTO(JobExecutionInfo entity) {
        return JobExecutionToDTOConverter.convert(entity);
    }
}
