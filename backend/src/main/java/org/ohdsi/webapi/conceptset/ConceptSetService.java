package org.ohdsi.webapi.conceptset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.ohdsi.webapi.conceptset.converter.ConceptSetDTOToConceptSetConverter;
import org.ohdsi.webapi.conceptset.converter.ConceptSetToConceptSetVersionConverter;
import org.ohdsi.webapi.conceptset.dto.ConceptSetDTO;
import org.ohdsi.webapi.conceptset.dto.ConceptSetVersionFullDTO;
import org.ohdsi.webapi.exceptions.NoSuchConceptException;
import org.ohdsi.webapi.source.SourceEntity;
import org.ohdsi.webapi.source.SourceService;
import org.ohdsi.circe.vocabulary.ConceptSetExpression;
import org.ohdsi.vocabulary.Concept;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.converter.UserDtoToUserEntity;
import org.ohdsi.webapi.authorization.model.SecuredEntityType;
import org.ohdsi.webapi.tag.TagService;
import org.ohdsi.webapi.tag.dto.TagNameListRequestDTO;
import org.ohdsi.webapi.versioning.domain.ConceptSetVersion;
import org.ohdsi.webapi.versioning.service.ConceptSetVersionService;
import org.ohdsi.webapi.vocabulary.VocabularyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.NotFoundException;

@Service
public class ConceptSetService {

    private static final Logger log = LoggerFactory.getLogger(ConceptSetService.class);

    private final AuthorizationManager authorizationManager;
    private final ConceptSetRepository conceptSetRepository;
    private final ConceptSetToConceptSetVersionConverter conceptSetToConceptSetVersionConverter;
    private final ConceptSetVersionService versionService;
    private final ConceptSetItemRepository conceptSetItemRepository;
    private final TagService tagService;
    private final SourceService sourceService;
    private final VocabularyService vocabularyService;
    private final ConversionService conversionService;
    private final ConceptSetGenerationInfoRepository conceptSetGenerationInfoRepository;

    public ConceptSetService(
            AuthorizationManager authorizationManager,
            ConceptSetRepository conceptSetRepository,
            ConceptSetToConceptSetVersionConverter conceptSetToConceptSetVersionConverter,
            ConceptSetVersionService versionService,
            ConceptSetItemRepository conceptSetItemRepository,
            TagService tagService,
            SourceService sourceService,
            VocabularyService vocabularyService,
            ConversionService conversionService,
            ConceptSetGenerationInfoRepository conceptSetGenerationInfoRepository) {
        this.authorizationManager = authorizationManager;
        this.conceptSetRepository = conceptSetRepository;
        this.conceptSetToConceptSetVersionConverter = conceptSetToConceptSetVersionConverter;
        this.versionService = versionService;
        this.conceptSetItemRepository = conceptSetItemRepository;
        this.tagService = tagService;
        this.sourceService = sourceService;
        this.vocabularyService = vocabularyService;
        this.conversionService = conversionService;
        this.conceptSetGenerationInfoRepository = conceptSetGenerationInfoRepository;
    }

    public ConceptSet getConceptSet(final int id) {
        var optionalConceptSet = conceptSetRepository.findById(id);
        if (optionalConceptSet.isEmpty()) {
            throw new NotFoundException(String.format("There is no concept set with id = %d.", id));
        }
        return optionalConceptSet.get();
    }

    public List<ConceptSet> getAllConceptSets() {
        var conceptSets = conceptSetRepository.findAll();
        return ((List<ConceptSet>)conceptSets).stream()
            .collect(Collectors.toList());
    }

    public List<ConceptSetItem> getConceptSetItems(final int conceptSetId) {
        return conceptSetItemRepository.findAllByConceptSetId(conceptSetId);
    }

    public ConceptSetVersionFullDTO getVersion(final int id, final int version) {
        var conceptSetVersion = versionService.getById(id, version);
        return conversionService.convert(conceptSetVersion, ConceptSetVersionFullDTO.class);
    }

    public ConceptSetExpression getConceptSetExpression(int id, Integer version, SourceEntity source) {
        HashMap<Long, Concept> map = new HashMap<>();
        // create our expression to return
        var expression = new ConceptSetExpression();
        var expressionItems = new ArrayList<ConceptSetExpression.ConceptSetItem>();

        var repositoryItems = new ArrayList<ConceptSetItem>();
        if (Objects.isNull(version)) {
            getConceptSetItems(id).forEach(repositoryItems::add);
        } else {
            var conceptSetVersion = getVersion(id, version);
            repositoryItems.addAll(conceptSetVersion.getItems());
        }

        // collect the unique concept IDs so we can load the concept object later.
        for (var csi : repositoryItems) {
            map.put(csi.getConceptId(), null);
        }

        // lookup the concepts we need information for
        var identifiers = new long[map.size()];
        int identifierIndex = 0;
        for (var identifier : map.keySet()) {
            identifiers[identifierIndex] = identifier;
            identifierIndex++;
        }

        var sourceToUse = source == null
                ? sourceService.getPriorityVocabularySource()
                : source;

        var concepts = vocabularyService.executeIdentifierLookup(sourceToUse, identifiers);
        if (concepts.size() != identifiers.length) {
            var ids = Arrays.stream(identifiers).boxed()
                    .filter(identifier -> concepts.stream().noneMatch(c -> c.conceptId.equals(identifier)))
                    .map(String::valueOf)
                    .collect(Collectors.joining(",", "(", ")"));
            throw new NoSuchConceptException("Current data source does not contain required concepts " + ids);
        }
        for (var concept : concepts) {
            map.put(concept.conceptId, concept); // associate the concept object to the conceptID in the map
        }

        // put the concept information into the expression along with the concept set item information
        for (var repositoryItem : repositoryItems) {
            var currentItem = new ConceptSetExpression.ConceptSetItem();
            currentItem.concept = map.get(repositoryItem.getConceptId());
            currentItem.includeDescendants = (repositoryItem.getIncludeDescendants() == 1);
            currentItem.includeMapped = (repositoryItem.getIncludeMapped() == 1);
            currentItem.isExcluded = (repositoryItem.getIsExcluded() == 1);
            expressionItems.add(currentItem);
        }
        expression.items = expressionItems.toArray(
                new ConceptSetExpression.ConceptSetItem[0]); // this will return a new array

        return expression;
    }

    public int getCountOfConceptSetsWithSameName(final int id, String name) {
        return conceptSetRepository.getCountCSetWithSameName(id, name);
    }


    public void assignTag(final Integer conceptSetId, final int tagId) {
        var optionalEntity = conceptSetRepository.findById(conceptSetId);
        if (optionalEntity.isEmpty()) {
            throw new NotFoundException("Concept Set " + conceptSetId + " does not exist.");
        }
        var conceptSet = optionalEntity.get();
        tagService.assignTag(conceptSet, tagId);
    }

    public void assignTag(List<Integer> ids, int tagId) {
        ids.forEach(id -> {
            try {
                assignTag(id, tagId);
            } catch (AccessDeniedException e) {
                log.warn("Tag {} cannot be assigned to entity {} - forbidden", tagId, id);
            }
        });
    }

    public void unassignTag(final Integer id, final int tagId) {
        var optionalEntity = conceptSetRepository.findById(id);
        if (optionalEntity.isEmpty()) {
            throw new NotFoundException("Concept Set " + id + " does not exist.");
        }
        var entity = optionalEntity.get();
        tagService.unassignTag(entity, tagId);
    }

    public void unassignTag(List<Integer> ids, int tagId) {
        ids.forEach(id -> {
            try {
                unassignTag(id, tagId);
            } catch (AccessDeniedException e) {
                log.warn("Tag {} cannot be unassigned from entity {} - forbidden", tagId, id);
            }
        });
    }

    public List<ConceptSet> listByTags(TagNameListRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.names() == null || requestDTO.names().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = requestDTO.names().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
        var entities = conceptSetRepository.findByTags(names);
        return tagService.listByTags(entities, names);
    }

    public ConceptSetDTO createConceptSet(ConceptSetDTO conceptSetDTO) {
        var currentUser = authorizationManager.getCurrentUser();
        var conceptSet = ConceptSetDTOToConceptSetConverter.convert(conceptSetDTO);
        var updated = new ConceptSet();
        updated.setCreatedBy(UserDtoToUserEntity.convert(currentUser));
        updated.setCreatedDate(new Date());
        updated.setTags(null);
        updateConceptSet(updated, conceptSet);
        authorizationManager.addOwnership(updated);
        return conversionService.convert(updated, ConceptSetDTO.class);
    }

    public ConceptSetDTO updateConceptSet(final int id, ConceptSetDTO conceptSetDTO) throws Exception {
        var optionalUpdated = conceptSetRepository.findById(id);
        if (optionalUpdated.isEmpty()) {
            throw new Exception("Concept Set " + id + " does not exist.");
        }
        saveVersion(id);
        var conceptSet = ConceptSetDTOToConceptSetConverter.convert(conceptSetDTO);
        var updated = updateConceptSet(optionalUpdated.get(), conceptSet);
        return conversionService.convert(updated, ConceptSetDTO.class);
    }

    public boolean saveConceptSetItems(final int id, ConceptSetItem[] items) {
        conceptSetItemRepository.deleteByConceptSetId(id);

        for (var csi : items) {
            // ID must be set to null in case of copying from version, so the new item will be created
            csi.setId(0);
            csi.setConceptSetId(id);
            conceptSetItemRepository.save(csi);
        }

        return true;
    }

    public List<String> getNamesLike(String copyName) {
        return conceptSetRepository.findAllByNameStartsWith(copyName).stream().map(ConceptSet::getName)
                .collect(Collectors.toList());
    }

    public ConceptSetExport getConceptSetForExport(int conceptSetId, SourceEntity vocabSource) {
        var cs = new ConceptSetExport();

        // Set the concept set id
        cs.ConceptSetId = conceptSetId;
        // Get the concept set information
        cs.ConceptSetName = this.getConceptSet(conceptSetId).getName();
        // Get the concept set expression
        cs.csExpression = this.getConceptSetExpression(conceptSetId, null, vocabSource);

        // Lookup the identifiers
        cs.identifierConcepts = vocabularyService.executeIncludedConceptLookup(vocabSource.getSourceKey(),
                cs.csExpression); //vocabService.executeIdentifierLookup(vocabSource.sourceKey, conceptIds);
        // Lookup the mapped items
        cs.mappedConcepts = vocabularyService.executeMappedLookup(vocabSource.getSourceKey(), cs.csExpression);

        return cs;
    }

    public void deleteConceptSet(final int id) {
        // Remove any generation info
        try {
            this.conceptSetGenerationInfoRepository.deleteByConceptSetId(id);
        } catch (EmptyResultDataAccessException e) {
            // Ignore - there may be no data
            log.warn("No generation info to delete for ConceptSet with ID = {}", id, e);
        } catch (Exception e) {
            throw e;
        }

        // Remove the concept set items
        try {
            conceptSetItemRepository.deleteByConceptSetId(id);
        } catch (EmptyResultDataAccessException e) {
            // Ignore - there may be no data
            log.warn("No items to delete for ConceptSet with ID = {}", id, e);
        } catch (Exception e) {
            throw e;
        }

        // Remove the concept set
        try {
            conceptSetRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            // Ignore - there may be no data
            log.warn("No ConceptSet to delete with ID = {}", id, e);
        } catch (Exception e) {
            throw e;
        }

        authorizationManager.removeOwnership(SecuredEntityType.CONCEPT_SET, id);
    }

    private ConceptSet updateConceptSet(ConceptSet dst, ConceptSet src) {
        var currentUser = authorizationManager.getCurrentUser();
        dst.setName(src.getName());
        dst.setDescription(src.getDescription());
        dst.setModifiedDate(new Date());
        dst.setModifiedBy(UserDtoToUserEntity.convert(currentUser));
        dst = conceptSetRepository.save(dst);
        return dst;
    }

    private ConceptSetVersion saveVersion(int id) {
        var optionalDef = conceptSetRepository.findById(id);
        if (optionalDef.isEmpty()) {
            throw new NotFoundException("Concept Set " + id + " does not exist.");
        }

        var def = optionalDef.get();
        var version = this.conceptSetToConceptSetVersionConverter.convert(def);

        var user = Objects.nonNull(def.getModifiedBy()) ? def.getModifiedBy() : def.getCreatedBy();
        var versionDate = Objects.nonNull(def.getModifiedDate()) ? def.getModifiedDate() : def.getCreatedDate();
        version.setCreatedBy(user);
        version.setCreatedDate(versionDate);
        return versionService.create(version);
    }

}
