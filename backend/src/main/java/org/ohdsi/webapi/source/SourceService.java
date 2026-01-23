package org.ohdsi.webapi.source;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.NotFoundException;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.sql.SqlTranslate;
import org.ohdsi.webapi.authorization.AnonymousUser;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.Permissions;
import org.ohdsi.webapi.authorization.converter.UserDtoToUserEntity;
import org.ohdsi.webapi.authorization.event.AddDataSourceEvent;
import org.ohdsi.webapi.authorization.event.ChangeDataSourceEvent;
import org.ohdsi.webapi.authorization.event.DeleteDataSourceEvent;
import org.ohdsi.webapi.authorization.service.PermissionService;
import org.ohdsi.webapi.authorization.service.RoleService;
import org.ohdsi.webapi.authorization.usercache.UserCache;
import org.ohdsi.webapi.common.SourceMapKey;
import org.ohdsi.webapi.exceptions.SourceDuplicateKeyException;
import org.ohdsi.webapi.model.DBMSType;
import org.ohdsi.webapi.util.CancelableJdbcTemplate;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SourceService {

    private static Collection<SourceEntity> cachedSources = null;
    private final Map<SourceEntity, Boolean> connectionAvailability = Collections.synchronizedMap(new PassiveExpiringMap<>(5000));

    private final SourceHelper sourceHelper;
    private final SourceRepository sourceRepository;
    private final WebApiProperties webApiProperties;
    private final AuthorizationManager authorizationManager;
    private final ApplicationEventPublisher publisher;
    private final SourceDaimonRepository sourceDaimonRepository;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final UserCache userCache;

    public SourceService(
            SourceRepository sourceRepository,
            SourceHelper sourceHelper,
            WebApiProperties webApiProperties,
            AuthorizationManager authorizationManager,
            ApplicationEventPublisher publisher,
            SourceDaimonRepository sourceDaimonRepository,
            RoleService roleService,
            PermissionService permissionService,
            UserCache userCache) {
        this.sourceRepository = sourceRepository;
        this.sourceHelper = sourceHelper;
        this.webApiProperties = webApiProperties;
        this.authorizationManager = authorizationManager;
        this.publisher = publisher;
        this.sourceDaimonRepository = sourceDaimonRepository;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.userCache = userCache;
    }

    @Transactional
    public Collection<SourceEntity> getSources() {
        if (cachedSources == null) {
            var sources = sourceRepository.findAll();
            sources.sort(new SortByKey());
            cachedSources = sources;
        }
        return cachedSources;
    }

    @Transactional
    public SourceEntity findBySourceKey(final String sourceKey) {
        return sourceRepository.findBySourceKey(sourceKey);
    }

    @Transactional
    public SourceEntity findBySourceId(final Integer sourceId) {
        return sourceRepository.findBySourceId(sourceId);
    }

    @Transactional
    public List<SourceEntity> findAll() {
        return sourceRepository.findAll();
    }

    @Transactional
    public <T> Map<T, SourceEntity> getSourcesMap(SourceMapKey<T> mapKey) {
        return getSources().stream().collect(Collectors.toMap(mapKey.getKeyFunc(), s -> s));
    }

    @Transactional
    public void checkConnection(SourceEntity source) {
        if (source.isCheckConnection()) {
            final JdbcTemplate jdbcTemplate = getSourceJdbcTemplate(source);
            jdbcTemplate.execute(
                    SqlTranslate.translateSql("select 1;", source.getSourceDialect()).replaceAll(";$", ""));
        }
    }

    @Transactional
    public SourceEntity getPrioritySourceForDaimon(SourceDaimonEntity.DaimonType daimonType) {
        List<SourceEntity> sourcesByDaimonPriority = sourceRepository.findAllSortedByDaimonPriority(daimonType);

        for (var source : sourcesByDaimonPriority) {
            if (!(authorizationManager.currentUserCanRead(source.getSourceKey())
                    || authorizationManager.currentUserHasPermission(Permissions.UPDATE_SOURCE)
                            && connectionAvailability.computeIfAbsent(source,
                                    this::checkConnectionSafe))) {
                continue;
            }
            return source;
        }

        return null;
    }

    @Transactional
    public Map<SourceDaimonEntity.DaimonType, SourceEntity> getPriorityDaimons() {

        class SourceValidator {
            private final Map<Integer, Boolean> checkedSources = new HashMap<>();

            private boolean isSourceAvailable(SourceEntity source) {
                return checkedSources.computeIfAbsent(source.getSourceId(),
                        v -> (authorizationManager.currentUserCanRead(source.getSourceKey())
                                || authorizationManager.currentUserHasPermission(Permissions.UPDATE_SOURCE))
                                && connectionAvailability.computeIfAbsent(source,
                                        SourceService.this::checkConnectionSafe));
            }
        }

        var sourceValidator = new SourceValidator();
        Map<SourceDaimonEntity.DaimonType, SourceEntity> priorityDaimons = new HashMap<>();
        Arrays.asList(SourceDaimonEntity.DaimonType.values()).forEach(d -> {

            var sources = sourceRepository.findAllSortedByDaimonPriority(d);
            Optional<SourceEntity> source = sources.stream().filter(sourceValidator::isSourceAvailable)
                    .findFirst();
            source.ifPresent(s -> priorityDaimons.put(d, s));
        });
        return priorityDaimons;
    }

    // TODO: The notion of the "priority vocabulary source" is weird and causes problems if
    // it's set to a source that some users can't access.
    @Transactional
    public SourceEntity getPriorityVocabularySource() {
        return getPrioritySourceForDaimon(SourceDaimonEntity.DaimonType.Vocabulary);
    }

    @Transactional
    public void updateSourcePriority(final String sourceKey, final String daimonTypeName) {
        var daimonType = SourceDaimonEntity.DaimonType.valueOf(daimonTypeName);
        var daimonList = sourceDaimonRepository.findByDaimonType(daimonType);
        daimonList.forEach(daimon -> {
            Integer newPriority = daimon.getSource().getSourceKey().equals(sourceKey) ? 1 : 0;
            daimon.setPriority(newPriority);
            sourceDaimonRepository.save(daimon);
        });
        invalidateCache();
    }

    public void invalidateCache() {
        cachedSources = null;
    }

    private boolean checkConnectionSafe(SourceEntity source) {
        try {
            checkConnection(source);
            return true;
        } catch (CannotGetJdbcConnectionException ex) {
            return false;
        }
    }

    private class SortByKey implements Comparator<SourceEntity> {
        private final boolean isAscending;

        public SortByKey(boolean ascending) {
            isAscending = ascending;
        }

        public SortByKey() {
            this(true);
        }

        public int compare(SourceEntity s1, SourceEntity s2) {
            return s1.getSourceKey().compareTo(s2.getSourceKey()) * (isAscending ? 1 : -1);
        }
    }

    public CancelableJdbcTemplate getSourceJdbcTemplate(SourceEntity source) {
        DriverManagerDataSource dataSource = sourceHelper.getDriverManagerDataSource(source);
        CancelableJdbcTemplate jdbcTemplate = new CancelableJdbcTemplate(dataSource);
        jdbcTemplate.setSuppressApiException(webApiProperties.cdmDb().jdbcTemplateSuppressApiExceptions());
        return jdbcTemplate;
    }

    @Transactional
    public SourceEntity createSource(SourceEntity source, MultipartFile file
    ) throws Exception {
        var sourceByKey = sourceRepository.findBySourceKey(source.getSourceKey());
        if (Objects.nonNull(sourceByKey)) {
            throw new SourceDuplicateKeyException("The source key has been already used.");
        }

        if (source.getDaimons() != null) {
            // First source should get priority = 1
            var sources = sourceRepository.findAll();
            source.getDaimons()
                    .stream()
                    .filter(sd -> sd.getPriority() <= 0)
                    .filter(sd -> {
                        boolean accept = true;
                        // Check if source daimon of given type with priority > 0 already exists in
                        // other sources
                        for (var innerSource : sources) {
                            accept = !innerSource.getDaimons()
                                    .stream()
                                    .anyMatch(innerDaimon -> innerDaimon.getPriority() > 0
                                            && innerDaimon.getDaimonType().equals(sd.getDaimonType()));
                            if (!accept) {
                                break;
                            }
                        }
                        return accept;
                    })
                    .forEach(sd -> sd.setPriority(1));
        }

        var original = new SourceEntity();
        original.setSourceDialect(source.getSourceDialect());
        setKeyfileData(source, original, file);

        var currentUser = authorizationManager.getCurrentUser();
        source.setCreatedBy(UserDtoToUserEntity.convert(currentUser));
        source.setCreatedDate(new Date());
        try {
            var saved = sourceRepository.saveAndFlush(source);
            authorizationManager.addOwnership(saved);
            addRoleForSource(saved);
            invalidateCache();
            publisher.publishEvent(new AddDataSourceEvent(this, source.getSourceId(), source.getSourceName()));
            return saved;
        } catch (PersistenceException ex) {
            throw new SourceDuplicateKeyException("You cannot use this Source Key, please use different one");
        }
    }

    @Transactional
    public SourceEntity updateSource(
            Integer sourceId,
            SourceEntity updated,
            Boolean checkConnection,
            MultipartFile file
    ) throws IOException {

        var source = sourceRepository.findBySourceId(sourceId);
        if (source != null) {
            updated.setSourceId(sourceId);
            updated.setSourceKey(source.getSourceKey());
            if (StringUtils.isBlank(updated.getUsername()) ||
                    Objects.equals(updated.getUsername().trim(), SourceEntity.MASKED_USERNAME)) {
                updated.setUsername(source.getUsername());
            }
            if (StringUtils.isBlank(updated.getPassword()) ||
                    Objects.equals(updated.getPassword().trim(), SourceEntity.MASKED_PASSWORD)) {
                updated.setPassword(source.getPassword());
            }
            if (StringUtils.isBlank(updated.getSourceConnection()) ||
                    Objects.equals(updated.getSourceConnection().trim(), SourceEntity.MASKED_CONNECTION_STRING)) {
                updated.setSourceConnection(source.getSourceConnection());
            }
            setKeyfileData(updated, source, file);
            transformIfRequired(updated);
            if (checkConnection == null) {
                updated.setCheckConnection(source.isCheckConnection());
            }

            var currentUser = authorizationManager.getCurrentUser();
            updated.setModifiedBy(UserDtoToUserEntity.convert(currentUser));
            updated.setModifiedDate(new Date());

            reuseDeletedDaimons(updated, source);

            var removed = source.getDaimons().stream().filter(d -> !updated.getDaimons().contains(d))
                    .collect(Collectors.toList());
            // Delete MUST be called after fetching user or source data to prevent autoflush
            // (see DefaultPersistEventListener.onPersist)
            sourceDaimonRepository.deleteAll(removed);
            var result = sourceRepository.save(updated);
            publisher.publishEvent(new ChangeDataSourceEvent(this, updated.getSourceId(), updated.getSourceName()));
            invalidateCache();
            return result;
        } else {
            throw new NotFoundException();
        }
    }

    @Transactional
    public void delete(Integer sourceId) throws Exception {
        var source = sourceRepository.findBySourceId(sourceId);
        if (source != null) {
            sourceRepository.delete(source);
            deleteRoleForSource(source.getSourceKey());
            publisher.publishEvent(new DeleteDataSourceEvent(this, sourceId, source.getSourceName()));
            invalidateCache();
            return;
        } else {
            throw new NotFoundException();
        }
    }

    public List<DatabaseTypeDto> getSupportedDialects() {
        return Arrays.asList(
            new DatabaseTypeDto(DBMSType.REDSHIFT.getOhdsiDB(), DBMSType.REDSHIFT.getLabel(), "configuration.viewEdit.dialect.options.redshift"),
            new DatabaseTypeDto(DBMSType.POSTGRESQL.getOhdsiDB(), DBMSType.POSTGRESQL.getLabel(), "configuration.viewEdit.dialect.options.postgresql")
        );
    }

    private void setKeyfileData(SourceEntity updated, SourceEntity source, MultipartFile file) throws IOException {
        if (source.supportsKeyfile()) {
            if (updated.getKeyfileName() != null) {
                if (!Objects.equals(updated.getKeyfileName(), source.getKeyfileName())) {
                    byte[] fileBytes = IOUtils.toByteArray(file.getInputStream());
                    updated.setKeyfile(fileBytes);
                } else {
                    updated.setKeyfile(source.getKeyfile());
                }
                return;
            }
        }
        updated.setKeyfile(null);
        updated.setKeyfileName(null);
    }

    private void transformIfRequired(SourceEntity source) {
        if (DBMSType.BIGQUERY.getOhdsiDB().equals(source.getSourceDialect())
                && ArrayUtils.isNotEmpty(source.getKeyfile())) {
            var connStr = source.getSourceConnection().replaceAll("OAuthPvtKeyPath=.+?(;|\\z)", "");
            source.setSourceConnection(connStr);
        }
    }

    private void reuseDeletedDaimons(SourceEntity updated, SourceEntity source) {
        var daimons = updated.getDaimons().stream().filter(d -> source.getDaimons().contains(d))
                .collect(Collectors.toList());
        var newDaimons = updated.getDaimons().stream().filter(d -> !source.getDaimons().contains(d))
                .toList();

        var allDaimons = sourceDaimonRepository.findBySource(source);

        for (var newSourceDaimon : newDaimons) {
            Optional<SourceDaimonEntity> reusedDaimonOpt = allDaimons.stream()
                    .filter(d -> d.equals(newSourceDaimon))
                    .findFirst();
            if (reusedDaimonOpt.isPresent()) {
                var reusedDaimon = reusedDaimonOpt.get();
                reusedDaimon.setPriority(newSourceDaimon.getPriority());
                reusedDaimon.setTableQualifier(newSourceDaimon.getTableQualifier());
                daimons.add(reusedDaimon);
            } else {
                daimons.add(newSourceDaimon);
            }
        }
        updated.setDaimons(daimons);
    }

    private void addRoleForSource(SourceEntity saved) {
        var permission = permissionService.createPermission(
            "source:" + saved.getSourceKey() + ":read",
            "Permission to access source " + saved.getSourceName()
        );
        var role = roleService.createRole(
            "Source user (" + saved.getSourceKey() + ")",
            true
        );
        roleService.addPermissionsToRole(role.getId(), Long.toString(permission.getId()));

        // Add the new role to the anonymous user
        roleService.addRoleToUser(AnonymousUser.ENTITY_ID, role);
    }

    private void deleteRoleForSource(String sourceKey) {
        userCache.clear();
        permissionService.deletePermissionByValue("source:" + sourceKey + ":read");
        roleService.deleteByNameAndSystemRole("Source user (" + sourceKey + ")", true);
    }
}
