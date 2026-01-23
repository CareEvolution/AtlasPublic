package org.ohdsi.webapi.common;

import org.ohdsi.webapi.cohortdefinition.*;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.converter.UserDtoToUserEntity;
import org.ohdsi.webapi.util.NameUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;

@Service
public class DesignImportService {
    private final AuthorizationManager authorizationManager;
    private final CohortDefinitionRepository cohortRepository;
    private final CohortDefinitionDetailsRepository detailsRepository;
    private final CohortDefinitionService cohortDefinitionService;

    public DesignImportService(
            AuthorizationManager authorizationManager,
            CohortDefinitionRepository cohortRepository,
            CohortDefinitionDetailsRepository detailsRepository,
            CohortDefinitionService cohortDefinitionService) {
        this.authorizationManager = authorizationManager;
        this.cohortRepository = cohortRepository;
        this.detailsRepository = detailsRepository;
        this.cohortDefinitionService = cohortDefinitionService;
    }

    public CohortDefinitionEntity persistCohortOrGetExisting(final CohortDefinitionEntity cohort) {
        return this.persistCohortOrGetExisting(cohort, false);
    }

    public CohortDefinitionEntity persistCohortOrGetExisting(final CohortDefinitionEntity cohort,
            final Boolean includeCohortNameInComparison) {
        final CohortDefinitionDetails details = cohort.getDetails();
        var findCohortResult = includeCohortNameInComparison
                ? this.findCohortByExpressionHashcodeAndName(details, cohort.getName())
                : this.findCohortByExpressionHashcode(details);
        return findCohortResult.orElseGet(() -> {
            var currentUser = authorizationManager.getCurrentUser();
            cohort.setCreatedBy(UserDtoToUserEntity.convert(currentUser));
            cohort.setCreatedDate(new Date());
            cohort.setDetails(details);
            details.setCohortDefinition(cohort);
            cohort.setName(NameUtils.getNameWithSuffix(cohort.getName(), this::getCdNamesLike));
            final var savedCohort = cohortRepository.save(cohort);
            detailsRepository.save(details);
            return savedCohort;
        });
    }

    private List<String> getCdNamesLike(String name) {
        return cohortDefinitionService.getNamesLike(name);
    }

    private Optional<CohortDefinitionEntity> findCohortByExpressionHashcode(final CohortDefinitionDetails details) {
        return this.findCohortByExpressionHashcodeAndPredicate(details, (c -> true));
    }

    private Optional<CohortDefinitionEntity> findCohortByExpressionHashcodeAndName(final CohortDefinitionDetails details,
            final String cohortName) {
        return this.findCohortByExpressionHashcodeAndPredicate(details, c -> Objects.equals(c.getName(), cohortName));
    }

    private Optional<CohortDefinitionEntity> findCohortByExpressionHashcodeAndPredicate(final CohortDefinitionDetails details,
            final Predicate<CohortDefinitionEntity> c) {
        var detailsFromDb = detailsRepository.findByHashCode(details.calculateHashCode());
        return detailsFromDb
                .stream()
                .filter(v -> Objects.equals(v.getStandardizedExpression(), details.getStandardizedExpression()))
                .map(CohortDefinitionDetails::getCohortDefinition)
                .filter(c)
                .findFirst();
    }
}