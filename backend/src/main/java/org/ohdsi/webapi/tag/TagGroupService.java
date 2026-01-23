package org.ohdsi.webapi.tag;

import org.ohdsi.webapi.cohortcharacterization.CcService;
import org.ohdsi.webapi.cohortdefinition.CohortDefinitionService;
import org.ohdsi.webapi.conceptset.ConceptSetService;
import org.ohdsi.webapi.tag.dto.TagGroupSubscriptionDTO;
import org.ohdsi.webapi.tag.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@Transactional
public class TagGroupService {
    private final TagRepository tagRepository;
    private final CcService ccService;
    private final CohortDefinitionService cohortDefinitionService;
    private final ConceptSetService conceptSetService;

    public TagGroupService(
            TagRepository tagRepository,
            CcService ccService,
            CohortDefinitionService cohortDefinitionService,
            ConceptSetService conceptSetService) {
        this.tagRepository = tagRepository;
        this.ccService = ccService;
        this.cohortDefinitionService = cohortDefinitionService;
        this.conceptSetService = conceptSetService;
    }

    @Transactional
    public void assignGroup(TagGroupSubscriptionDTO dto) {
        tagRepository.findByIdIn(new ArrayList<>(dto.getTags()))
                .forEach(tag -> {
                    ccService.assignTag(dto.getAssets().characterizations(), tag.getId());
                    cohortDefinitionService.assignTag(dto.getAssets().cohorts(), tag.getId());
                    conceptSetService.assignTag(dto.getAssets().conceptSets(), tag.getId());
                });
    }

    @Transactional
    public void unassignGroup(TagGroupSubscriptionDTO dto) {
        tagRepository.findByIdIn(new ArrayList<>(dto.getTags()))
                .forEach(tag -> {
                    ccService.unassignTag(dto.getAssets().characterizations(), tag.getId());
                    cohortDefinitionService.unassignTag(dto.getAssets().cohorts(), tag.getId());
                    conceptSetService.unassignTag(dto.getAssets().conceptSets(), tag.getId());
                });
    }
}
