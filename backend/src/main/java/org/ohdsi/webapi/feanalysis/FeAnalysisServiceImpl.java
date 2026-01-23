package org.ohdsi.webapi.feanalysis;

import jakarta.ws.rs.NotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.ohdsi.analysis.cohortcharacterization.design.CcResultType;
import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.converter.UserDtoToUserEntity;
import org.ohdsi.webapi.authorization.model.SecuredEntityType;
import org.ohdsi.webapi.cohortcharacterization.domain.CohortCharacterizationEntity;
import org.ohdsi.webapi.conceptset.ConceptSetExport;
import org.ohdsi.webapi.feanalysis.domain.*;
import org.ohdsi.webapi.feanalysis.event.FeAnalysisChangedEvent;
import org.ohdsi.webapi.feanalysis.repository.FeAnalysisAggregateRepository;
import org.ohdsi.webapi.feanalysis.repository.FeAnalysisCriteriaRepository;
import org.ohdsi.webapi.feanalysis.repository.FeAnalysisEntityRepository;
import org.ohdsi.webapi.feanalysis.repository.FeAnalysisWithStringEntityRepository;
import org.ohdsi.webapi.source.SourceInfo;
import org.ohdsi.webapi.util.NameUtils;
import org.ohdsi.webapi.vocabulary.VocabularyService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// TODO fix raw use of parameterized classes FeAnalysisEntity, FeAnalysisWithCriteriaEntity
@Service
@Transactional(readOnly = true)
public class FeAnalysisServiceImpl implements FeAnalysisService {
    private final FeAnalysisEntityRepository analysisRepository;
    private final FeAnalysisCriteriaRepository criteriaRepository;
    private final FeAnalysisWithStringEntityRepository stringAnalysisRepository;
    private final VocabularyService vocabularyService;
    private final AuthorizationManager authorizationManager;

    private final ApplicationEventPublisher eventPublisher;
    private final FeAnalysisAggregateRepository aggregateRepository;

    public FeAnalysisServiceImpl(
            final FeAnalysisEntityRepository analysisRepository,
            final FeAnalysisCriteriaRepository criteriaRepository,
            final FeAnalysisWithStringEntityRepository stringAnalysisRepository,
            final VocabularyService vocabularyService,
            final FeAnalysisAggregateRepository aggregateRepository,
            final ApplicationEventPublisher eventPublisher,
            final AuthorizationManager authorizationManager) {
        this.analysisRepository = analysisRepository;
        this.criteriaRepository = criteriaRepository;
        this.stringAnalysisRepository = stringAnalysisRepository;
        this.vocabularyService = vocabularyService;
        this.aggregateRepository = aggregateRepository;
        this.eventPublisher = eventPublisher;
        this.authorizationManager = authorizationManager;
    }

    @Override
    public Page<FeAnalysisEntity> getPage(final Pageable pageable) {
        return analysisRepository.findAll(pageable);
    }

    @Override
    public int getCountFeWithSameName(Integer id, String name) {
        return analysisRepository.getCountFeWithSameName(id, name);
    }

    @Override
    public List<FeAnalysisWithStringEntity> findPresetAnalysesBySystemNames(Collection<String> names) {
        return stringAnalysisRepository.findByDesignIn(names);
    }

    @Override
    @Transactional
    public FeAnalysisEntity createAnalysis(final FeAnalysisEntity analysis) {
        if (analysis.getStatType() == null) {
            analysis.setStatType(CcResultType.PREVALENCE);
        }
        return saveNew(analysis);
    }

    @Override
    public Optional<FeAnalysisEntity> findById(Integer id) {
        return analysisRepository.findByIdWithUserInfo(id);
    }

    @Override
    public Optional<FeAnalysisEntity> findByName(String name) {
        return analysisRepository.findByName(name);
    }

    @Override
    @Transactional
    public FeAnalysisWithCriteriaEntity createCriteriaAnalysis(final FeAnalysisWithCriteriaEntity analysis) {
        FeAnalysisWithCriteriaEntity newAnalysis = newAnalysis(analysis);
        newAnalysis.setDesign(Collections.emptyList());
        final FeAnalysisWithCriteriaEntity entityWithMainFields = saveNew(newAnalysis);
        if (createOrUpdateConceptSetEntity(entityWithMainFields, analysis.getConceptSetEntity())) {
            analysisRepository.save(entityWithMainFields);
        }
        final List<FeAnalysisCriteriaEntity> criteriaList = createCriteriaListForAnalysis(entityWithMainFields,
                analysis.getDesign());
        entityWithMainFields.setDesign(criteriaList);
        return entityWithMainFields;
    }

    private boolean createOrUpdateConceptSetEntity(FeAnalysisWithCriteriaEntity analysis,
            FeAnalysisConcepsetEntity modifiedConceptSet) {
        if (Objects.nonNull(modifiedConceptSet)) {
            FeAnalysisConcepsetEntity conceptSet = Optional.ofNullable(analysis.getConceptSetEntity())
                    .orElseGet(FeAnalysisConcepsetEntity::new);
            conceptSet.setFeatureAnalysis(analysis);
            conceptSet.setRawExpression(modifiedConceptSet.getRawExpression());
            analysis.setConceptSetEntity(conceptSet);
            return true;
        } else {
            return false;
        }
    }

    private <T extends FeAnalysisEntity> T saveNew(T entity) {
        var userDto = authorizationManager.getCurrentUser();
        var userEntity = UserDtoToUserEntity.convert(userDto);
        entity.setCreatedBy(userEntity);
        entity.setCreatedDate(new Date());
        var savedEntity = analysisRepository.saveAndFlush(entity);
        authorizationManager.addOwnership(savedEntity);
        return savedEntity;
    }

    private FeAnalysisWithCriteriaEntity newAnalysis(final FeAnalysisWithCriteriaEntity analysis) {
        if (Objects.equals(analysis.getStatType(), CcResultType.PREVALENCE)) {
            return new FeAnalysisWithPrevalenceCriteriaEntity(analysis);
        } else if (Objects.equals(analysis.getStatType(), CcResultType.DISTRIBUTION)) {
            return new FeAnalysisWithDistributionCriteriaEntity(analysis);
        }
        throw new IllegalArgumentException();
    }

    private List<FeAnalysisCriteriaEntity> createCriteriaListForAnalysis(final FeAnalysisWithCriteriaEntity analysis,
            final List<FeAnalysisCriteriaEntity> design) {
        return design.stream()
                .peek(criteria -> criteria.setFeatureAnalysis(analysis))
                .map(criteria -> criteriaRepository.save(criteria))
                .collect(Collectors.toList());
    }

    @Override
    public Set<FeAnalysisEntity> findByCohortCharacterization(
            final CohortCharacterizationEntity cohortCharacterization) {
        return analysisRepository.findAllByCohortCharacterizations(cohortCharacterization);
    }

    @Override
    public List<FeAnalysisWithStringEntity> findAllPresetAnalyses() {
        return analysisRepository.findAllByType(StandardFeatureAnalysisType.PRESET).stream()
                .map(a -> (FeAnalysisWithStringEntity) a).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FeAnalysisEntity updateAnalysis(Integer feAnalysisId, FeAnalysisEntity updatedEntity) {
        var savedEntity = findById(feAnalysisId).orElseThrow(NotFoundException::new);

        checkEntityLocked(savedEntity);
        savedEntity.setDescr(updatedEntity.getDescr());
        if (savedEntity instanceof FeAnalysisWithCriteriaEntity && updatedEntity instanceof FeAnalysisWithCriteriaEntity) {
            FeAnalysisWithCriteriaEntity<?> updatedWithCriteriaEntity = (FeAnalysisWithCriteriaEntity) updatedEntity,
                    savedWithCriteria = (FeAnalysisWithCriteriaEntity) savedEntity;
            removeFeAnalysisCriteriaEntities(savedWithCriteria, updatedWithCriteriaEntity);
            updatedWithCriteriaEntity.getDesign().forEach(criteria -> criteria.setFeatureAnalysis(savedWithCriteria));
            createOrUpdateConceptSetEntity((FeAnalysisWithCriteriaEntity) savedEntity,
                    updatedWithCriteriaEntity.getConceptSetEntity());
        }
        savedEntity.setDesign(updatedEntity.getDesign());
        if (Objects.nonNull(updatedEntity.getDomain())) {
            savedEntity.setDomain(updatedEntity.getDomain());
        }
        savedEntity.setLocked(updatedEntity.getLocked());
        if (StringUtils.isNotEmpty(updatedEntity.getName())) {
            savedEntity.setName(updatedEntity.getName());
        }
        if (updatedEntity.getStatType() != null) {
            savedEntity.setStatType(updatedEntity.getStatType());
        }
        if (Objects.nonNull(updatedEntity.getType())) {
            savedEntity.setType(updatedEntity.getType());
        }
        var userDto = authorizationManager.getCurrentUser();
        var userEntity = UserDtoToUserEntity.convert(userDto);
        savedEntity.setModifiedBy(userEntity);
        savedEntity.setModifiedDate(new Date());
        savedEntity = analysisRepository.save(savedEntity);
        eventPublisher.publishEvent(new FeAnalysisChangedEvent(savedEntity));
        return savedEntity;
    }

    private void removeFeAnalysisCriteriaEntities(FeAnalysisWithCriteriaEntity<?> original,
            FeAnalysisWithCriteriaEntity<?> updated) {

        List<FeAnalysisCriteriaEntity> removed = original.getDesign().stream()
                .filter(c -> updated.getDesign().stream().noneMatch(u -> Objects.equals(c.getId(), u.getId())))
                .collect(Collectors.toList());
        criteriaRepository.deleteAll(removed);
    }

    @Override
    @Transactional
    public void deleteAnalysis(FeAnalysisEntity<?> entity) {
        var entityId = entity.getId();
        checkEntityLocked(entity);
        analysisRepository.delete(entity);
        authorizationManager.removeOwnership(SecuredEntityType.FE_ANALYSIS, entityId);
    }

    @Override
    @Transactional
    public void deleteAnalysis(int id) {
        deleteAnalysis(analysisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("There is no Feature Analysis with id = " + id)));
    }

    @Override
    public List<String> getNamesLike(String name) {
        return analysisRepository.findAllByNameStartsWith(name).stream().map(FeAnalysisEntity::getName)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConceptSetExport> exportConceptSets(FeAnalysisWithCriteriaEntity<?> analysisEntity) {
        var sourceInfo = new SourceInfo(vocabularyService.getPriorityVocabularySource());
        var conceptSets = analysisEntity.getConceptSets();
        return conceptSets.stream()
                .map(cs -> vocabularyService.exportConceptSet(cs, sourceInfo))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<? extends FeAnalysisEntity> findByDesignAndName(final FeAnalysisWithStringEntity withStringEntity,
            final String name) {
        return this.findByDesignAndPredicate(withStringEntity.getDesign(), f -> Objects.equals(f.getName(), name));
    }

    @Override
    public Optional<FeAnalysisEntity> findByCriteriaListAndCsAndDomainAndStat(
            List<? extends FeAnalysisCriteriaEntity> newCriteriaList,
            FeAnalysisWithCriteriaEntity<? extends FeAnalysisCriteriaEntity> newFeAnalysis) {
        Map<FeAnalysisWithCriteriaEntity, List<FeAnalysisCriteriaEntity>> feAnalysisEntityListMap = newCriteriaList.stream()
                .map(c -> criteriaRepository.findAllByExpressionString(c.getExpressionString()))
                .flatMap(List::stream).collect(Collectors.groupingBy(FeAnalysisCriteriaEntity::getFeatureAnalysis));
        return feAnalysisEntityListMap.entrySet().stream().filter(e -> {
            FeAnalysisWithCriteriaEntity feAnalysis = e.getKey();
            return checkCriteriaList(e.getValue(), newCriteriaList) &&
                    CollectionUtils.isEqualCollection(feAnalysis.getConceptSets(), newFeAnalysis.getConceptSets()) &&
                    feAnalysis.getDomain().equals(newFeAnalysis.getDomain()) &&
                    feAnalysis.getStatType().equals(newFeAnalysis.getStatType());
        }).findAny().map(Map.Entry::getKey);
    }

    private boolean checkCriteriaList(List<FeAnalysisCriteriaEntity> curCriteriaList,
            List<? extends FeAnalysisCriteriaEntity> newCriteriaList) {
        var currentList = curCriteriaList.stream().map(FeAnalysisCriteriaEntity::getExpressionString)
                .collect(Collectors.toList());
        var newList = newCriteriaList.stream().map(FeAnalysisCriteriaEntity::getExpressionString)
                .collect(Collectors.toList());
        return CollectionUtils.isEqualCollection(currentList, newList);
    }

    private Optional<? extends FeAnalysisEntity> findByDesignAndPredicate(final String design,
            final Predicate<FeAnalysisEntity> f) {
        List<? extends FeAnalysisEntity> detailsFromDb = stringAnalysisRepository.findByDesign(design);
        return detailsFromDb
                .stream()
                .filter(f)
                .findFirst();
    }

    private void checkEntityLocked(FeAnalysisEntity entity) {
        if (entity.getLocked() == Boolean.TRUE) {
            throw new IllegalArgumentException(String.format("Feature analysis %s is locked.", entity.getName()));
        }
    }

    @Override
    public List<FeAnalysisAggregateEntity> findAggregates() {
        return aggregateRepository.findAll();
    }

    @Override
    public FeAnalysisEntity getNewEntityForCopy(FeAnalysisEntity entity) {
        FeAnalysisEntity entityForCopy;
        switch (entity.getType()) {
            case CRITERIA_SET:
                entityForCopy = switch (entity.getStatType()) {
                    case PREVALENCE -> new FeAnalysisWithPrevalenceCriteriaEntity(
                            (FeAnalysisWithCriteriaEntity) entity);
                    case DISTRIBUTION -> new FeAnalysisWithDistributionCriteriaEntity(
                            (FeAnalysisWithCriteriaEntity) entity);
                    default -> throw new IllegalArgumentException();
                };

                // deep copy of criteria list...
                final List<FeAnalysisCriteriaEntity> criteriaList = new ArrayList<>();
                ((FeAnalysisWithCriteriaEntity) entity).getDesign().forEach(c -> {
                    final FeAnalysisCriteriaEntity criteria = createCriteriaEntity((FeAnalysisCriteriaEntity) c);
                    criteria.setName(((FeAnalysisCriteriaEntity) c).getName());
                    criteria.setExpressionString(((FeAnalysisCriteriaEntity) c).getExpressionString());
                    criteria.setAggregate(((FeAnalysisCriteriaEntity) c).getAggregate());
                    criteriaList.add(criteria);
                });
                entityForCopy.setDesign(criteriaList);

                // ...and concept sets
                final FeAnalysisConcepsetEntity concepsetEntity = new FeAnalysisConcepsetEntity();
                concepsetEntity.setRawExpression(
                        ((FeAnalysisWithCriteriaEntity) entity).getConceptSetEntity().getRawExpression());
                ((FeAnalysisWithCriteriaEntity) entityForCopy).setConceptSetEntity(concepsetEntity);
                break;
            case CUSTOM_FE:
                entityForCopy = new FeAnalysisWithStringEntity((FeAnalysisWithStringEntity) entity);
                break;
            default:
                throw new IllegalArgumentException("Analysis with type: " + entity.getType() + " cannot be copied");
        }
        entityForCopy.setId(null);
        entityForCopy.setName(
                NameUtils.getNameForCopy(entityForCopy.getName(), this::getNamesLike,
                        findByName(entityForCopy.getName())));
        entityForCopy.setCreatedBy(null);
        entityForCopy.setCreatedDate(null);
        entityForCopy.setModifiedBy(null);
        entityForCopy.setModifiedDate(null);
        return entityForCopy;
    }

    private FeAnalysisCriteriaEntity createCriteriaEntity(FeAnalysisCriteriaEntity basis) {
        if (basis instanceof FeAnalysisWindowedCriteriaEntity) {
            return new FeAnalysisWindowedCriteriaEntity();
        } else if (basis instanceof FeAnalysisDemographicCriteriaEntity) {
            return new FeAnalysisDemographicCriteriaEntity();
        } else {
            return new FeAnalysisCriteriaGroupEntity();
        }
    }
}
