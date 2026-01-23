/*
 * Copyright 2015 fdefalco.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ohdsi.webapi.person;

import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.Permissions;
import org.ohdsi.webapi.source.SourceDaimonEntity;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.webapi.util.PreparedStatementRenderer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * REST API for working with person profiles. This controller provides endpoints for retrieving a person's medical history,
 * including their demographics, observation periods, and records. The records are returned with start and end 
 * days relative to a cohort start date or the initial date of observation.
 */
@RestController
@RequestMapping("{sourceKey}/person")
public class PersonService {
    private final SourceService sourceService;
    private final AuthorizationManager authorizationManager;

    public PersonService(
            SourceService sourceService,
            AuthorizationManager authorizationManager) {
        this.sourceService = sourceService;
        this.authorizationManager = authorizationManager;
    }

    /**
     * Get the complete medical history for a single person in a single database
     *
     * @param sourceKey
     *         Data source to extract from
     * @param personId
     *         Person whose profile to extract
     * @param cohortId
     *         (optional) Cohort used to adjust start and end dates. If the person is a member of the cohort then start
     *         and end dates will be adjusted so they are relative to the cohort start date.
     * @return All records in a patient profile with start and end days relative to cohort start date or initial date of
     *         observation
     */
    @GetMapping("/{personId}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_PERSON) and @authorizationManager.currentUserHasPermission(@PERMS.READ_SOURCE)")
    public PersonProfile getPersonProfile(
        @PathVariable("sourceKey") String sourceKey,
        @PathVariable("personId") String personId,
        @RequestParam(name="cohort", defaultValue="0") Long cohortId
    ) {
        if (!authorizationManager.currentUserCanRead(sourceKey)) {
            throw new AccessDeniedException("User does not have permission to read this source");
        }

        final var profile = new PersonProfile();
        boolean showDates = canViewDates();

        var source = sourceService.findBySourceKey(sourceKey);
        profile.gender = "not found";
        profile.yearOfBirth = 0;

        var psrPersonInfo = preparePersonInfoSql(personId, source);
        sourceService.getSourceJdbcTemplate(source).query(psrPersonInfo.getSql(), psrPersonInfo.getSetter(),
                (RowMapper<Void>) (resultSet, arg1) -> {
                    profile.yearOfBirth = resultSet.getInt("year_of_birth");
                    profile.gender = resultSet.getString("gender");
                    return null;
                });
        if (profile.gender.equals("not found")) {
            throw new RuntimeException("Can't find person " + personId);
        }

        // get observation periods
        var psrObservationPeriods = prepareObservationPeriodsSql(personId, source);
        sourceService.getSourceJdbcTemplate(source).query(psrObservationPeriods.getSql(), psrObservationPeriods.getSetter(),
                (RowMapper<Void>) (resultSet, arg1) -> {
                    var op = new ObservationPeriod();

                    op.startDate = resultSet.getTimestamp("start_date");
                    op.endDate = resultSet.getTimestamp("end_date");
                    op.type = resultSet.getString("observation_period_type");
                    op.id = resultSet.getLong("observation_period_id");

                    profile.observationPeriods.add(op);
                    return null;
                });
        // get simplified records
        var psrPersonProfile = prepareGetPersonProfile(personId, source);
        sourceService.getSourceJdbcTemplate(source).query(psrPersonProfile.getSql(), psrPersonProfile.getSetter(),
                (RowMapper<Void>) (resultSet, arg1) -> {
                    var item = new PersonRecord();

                    item.conceptId = resultSet.getLong("concept_id");
                    item.conceptName = resultSet.getString("concept_name");
                    item.domain = resultSet.getString("domain");
                    item.startDate = resultSet.getTimestamp("start_date");
                    item.endDate = resultSet.getTimestamp("end_date");

                    profile.records.add(item);
                    return null;
                });

        var psrGetCohorts = prepareGetCohortsSql(personId, source);
        sourceService.getSourceJdbcTemplate(source).query(psrGetCohorts.getSql(), psrGetCohorts.getSetter(),
                (RowMapper<Void>) (resultSet, arg1) -> {
                    var item = new CohortPerson();

                    item.startDate = resultSet.getTimestamp("cohort_start_date");
                    item.endDate = resultSet.getTimestamp("cohort_end_date");
                    item.cohortDefinitionId = resultSet.getLong("cohort_definition_id");

                    profile.cohorts.add(item);
                    return null;
                });

        LocalDateTime cohortStartDate = null;
        Optional<CohortPerson> cohort = cohortId > 0
                ? profile.cohorts.stream().filter(c -> c.cohortDefinitionId.equals(cohortId)).findFirst()
                : Optional.empty();
        cohortStartDate = cohort.map(c -> c.startDate.toLocalDateTime())
                .orElseGet(() -> profile.records.stream().min(Comparator.comparing(c -> c.startDate))
                        .map(r -> r.startDate.toLocalDateTime()).orElse(null));

        if (cohortStartDate != null && profile.yearOfBirth > 0) {
            profile.ageAtIndex = cohortStartDate.getYear() - profile.yearOfBirth;
        }

        for (var record : profile.records) {
            record.startDay = Math.toIntExact(
                    ChronoUnit.DAYS.between(cohortStartDate, record.startDate.toLocalDateTime()));
            record.endDay = Objects.nonNull(record.endDate) ? Math.toIntExact(ChronoUnit.DAYS.between(cohortStartDate,
                    record.endDate.toLocalDateTime())) : record.startDay;
            if (!showDates) {
                record.startDate = null;
                record.endDate = null;
            }
        }
        for (var period : profile.observationPeriods) {
            period.x1 = Math.toIntExact(ChronoUnit.DAYS.between(cohortStartDate,
                    period.startDate.toLocalDateTime()));
            period.x2 = Math.toIntExact(ChronoUnit.DAYS.between(cohortStartDate,
                    period.endDate.toLocalDateTime()));
            if (!showDates) {
                period.startDate = null;
                period.endDate = null;
            }
        }

        return profile;
    }

    protected PreparedStatementRenderer prepareObservationPeriodsSql(String personId, SourceEntity source) {
        var replacementStrings = new String[] {"cdm_schema", "vocab_schema"};
        var replacementValues = new String[] {
                source.getTableQualifier(SourceDaimonEntity.DaimonType.CDM),
                source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary)
        };
        return new PreparedStatementRenderer(source, "person/sql/getObservationPeriods.sql",
                replacementStrings, replacementValues, "personId", Long.valueOf(personId));
    }

    protected PreparedStatementRenderer prepareGetCohortsSql(String personId, SourceEntity source) {
        var resultsTableQualifier = source.getTableQualifier(SourceDaimonEntity.DaimonType.Results);
        return new PreparedStatementRenderer(source, "person/sql/getCohorts.sql", "tableQualifier",
                resultsTableQualifier, "subjectId", Long.valueOf(personId));
    }

    protected PreparedStatementRenderer preparePersonInfoSql(String personId, SourceEntity source) {
        var cdmSchemaValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.CDM);
        var vocabSchemaValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var replaceStrings = new String[] {"cdm_schema", "vocab_schema"};
        var replaceValues = new String[] {cdmSchemaValue, vocabSchemaValue};
        return new PreparedStatementRenderer(source, "person/sql/personInfo.sql", replaceStrings,
                replaceValues, "personId", Long.valueOf(personId));
    }

    protected PreparedStatementRenderer prepareGetPersonProfile(String personId, SourceEntity source) {
        var cdmSchemaValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.CDM);
        var vocabSchemaValue = source.getTableQualifier(SourceDaimonEntity.DaimonType.Vocabulary);
        var replaceStrings = new String[] {"cdm_schema", "vocab_schema"};
        var replaceValues = new String[] {cdmSchemaValue, vocabSchemaValue};
        return new PreparedStatementRenderer(source, "/person/sql/getRecords.sql", replaceStrings, replaceValues,
                "personId", Long.valueOf(personId));
    }

    private Boolean canViewDates() {
        return authorizationManager.currentUserHasPermission(Permissions.READ_PERSON_DATES);
    }
}
