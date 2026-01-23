package org.ohdsi.webapi.tag;

import jakarta.persistence.EntityManager;
import jakarta.ws.rs.NotFoundException;

import org.glassfish.jersey.internal.util.Producer;
import org.ohdsi.webapi.authorization.model.SecuredEntityType;
import org.ohdsi.webapi.model.TaggableEntity;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.converter.UserDtoToUserEntity;
import org.ohdsi.webapi.tag.domain.Tag;
import org.ohdsi.webapi.tag.domain.TagInfo;
import org.ohdsi.webapi.tag.domain.TagType;
import org.ohdsi.webapi.tag.dto.TagDTO;
import org.ohdsi.webapi.tag.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TagService {
    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    private final AuthorizationManager authorizationManager;
    private final EntityManager entityManager;
    private final TagRepository tagRepository;

    private final ArrayList<Producer<List<TagInfo>>> infoProducers;

    public TagService(
            AuthorizationManager authorizationManager,
            EntityManager entityManager,
            TagRepository tagRepository) {
        this.authorizationManager = authorizationManager;
        this.entityManager = entityManager;
        this.tagRepository = tagRepository;

        this.infoProducers = new ArrayList<>();
        this.infoProducers.add(tagRepository::findCohortTagInfo);
        this.infoProducers.add(tagRepository::findCcTagInfo);
        this.infoProducers.add(tagRepository::findConceptSetTagInfo);
    }

    public Tag create(Tag tag) {
        tag.setType(TagType.CUSTOM);
        var groupIds = tag.getGroups().stream()
                .map(Tag::getId)
                .collect(Collectors.toList());
        var groups = findByIdIn(groupIds);

        tag.setGroups(new HashSet<>(groups));
        var currentUser = authorizationManager.getCurrentUser();
        tag.setCreatedBy(UserDtoToUserEntity.convert(currentUser));
        tag.setCreatedDate(new Date());

        var savedTag =  save(tag);
        authorizationManager.addOwnership(savedTag);
        return savedTag;
    }

    public Tag getById(Integer id) {
        var optionalTag = tagRepository.findById(id);
        if (optionalTag.isEmpty()) {
            throw new NotFoundException(String.format("Tag with id=%d not found", id));
        }
        return optionalTag.get();
    }

    public List<Tag> listInfo(String namePart) {
        return tagRepository.findAllTags(namePart);
    }

    public List<Tag> listInfo() {
        return tagRepository.findAll();
    }

    public List<Tag> findByIdIn(List<Integer> ids) {
        return tagRepository.findByIdIn(ids);
    }

    public Tag update(Integer id, Tag toUpdate) {
        var optionalExistingTag = tagRepository.findById(id);
        if (optionalExistingTag.isEmpty()) {
            throw new NotFoundException(String.format("Tag with id=%d not found", id));
        }

        var existingTag = optionalExistingTag.get();
        authorizationManager.throwIfCurrentUserDoesntOwn(SecuredEntityType.TAG, existingTag.getId());

        var groupIds = toUpdate.getGroups().stream()
                .map(Tag::getId)
                .collect(Collectors.toList());
        var groups = findByIdIn(groupIds);
        toUpdate.setGroups(new HashSet<>(groups));

        toUpdate.setCreatedBy(existingTag.getCreatedBy());
        toUpdate.setCreatedDate(existingTag.getCreatedDate());

        var currentUser = authorizationManager.getCurrentUser();
        toUpdate.setModifiedBy(UserDtoToUserEntity.convert(currentUser));
        toUpdate.setModifiedDate(new Date());

        var saved = save(toUpdate);
        return saved;
    }

    public void delete(Integer id) {
        var optionalTag = tagRepository.findById(id);
        if (optionalTag.isEmpty()) {
            throw new NotFoundException(String.format("Tag with id=%d not found", id));
        }

        var tag = optionalTag.get();
        authorizationManager.throwIfCurrentUserDoesntOwn(SecuredEntityType.TAG, tag.getId());
        tagRepository.delete(tag);
        authorizationManager.removeOwnership(SecuredEntityType.TAG, tag.getId());
    }

    private Tag save(Tag tag) {
        var updatedTag = tagRepository.saveAndFlush(tag);
        entityManager.refresh(updatedTag);

        var optionalTag = tagRepository.findById(updatedTag.getId());
        if (optionalTag.isEmpty()) {
            throw new NotFoundException(String.format("Tag with id=%d not found", updatedTag.getId()));
        }
        return optionalTag.get();
    }

    @Transactional
    @Scheduled(fixedDelayString = "${webapi.tag.refresh-stat-period-ms}")
    public void refreshTagStatistics() {
        logger.info("Starting tags statistics refreshing");
        try {
            // Getting tag statistics in one query with multiple join clauses
            // will take significant amount of time
            // So we'll get this information for each asset in series
            Map<Integer, TagDTO> infoMap = new HashMap<>();
            this.infoProducers.forEach(producer -> processTagInfo(producer, infoMap));

            var tags = tagRepository.findAll();
            tags = tags.stream()
                    .peek(tag -> {
                        TagDTO info = infoMap.get(tag.getId());
                        if (Objects.nonNull(info)) {
                            tag.setCount(info.getCount());
                        }
                    })
                    .collect(Collectors.toList());
            tagRepository.saveAll(tags);
        } catch (Exception e) {
            logger.error("Cannot refresh tags statistics");
        }
        logger.info("Finishing tags statistics refreshing");
    }

    public void assignTag(TaggableEntity<?> entity, int tagId) {
        if (Objects.isNull(entity)){
            return;
        }

        var tag = getById(tagId);
        if (Objects.isNull(tag)) {
            return;
        }

        if (tag.isPermissionProtected() && !hasPermissionToAssignProtectedTags(entity, "post")) {
            throw new AccessDeniedException(
                    String.format("No permission to assign protected tag '%s' to %s (id=%s).",
                            tag.getName(), entity.getClass().getSimpleName(), entity.getId()));
        }

        // unassign tags from the same group if group marked as multi_selection=false
        tag.getGroups().stream().findFirst().ifPresent(group -> {
            if (!group.isMultiSelection()) {
                entity.getTags().forEach(t -> {
                    if (t.getGroups().stream().anyMatch(g -> g.getId().equals(group.getId()))) {
                        unassignTag(entity, t.getId());
                    }
                });
            }
        });

        entity.getTags().add(tag);
    }

    public void unassignTag(TaggableEntity<?> entity, int tagId) {
        if (Objects.isNull(entity)) {
            return;
        }
        Tag tag = getById(tagId);
        if (Objects.isNull(tag)) {
            return;
        }
        if (tag.isPermissionProtected() && !hasPermissionToAssignProtectedTags(entity, "delete")) {
            throw new AccessDeniedException(
                    String.format("No permission to unassign protected tag '%s' from %s (id=%s).",
                            tag.getName(), entity.getClass().getSimpleName(), entity.getId()));
        }
        Set<Tag> tags = entity.getTags().stream()
                .filter(t -> t.getId() != tagId)
                .collect(Collectors.toSet());
        entity.setTags(tags);
    }

    private boolean hasPermissionToAssignProtectedTags(final TaggableEntity<?> entity, final String method) {
        return authorizationManager.currentUserCanUpdate(entity);
    }

    private void processTagInfo(Producer<List<TagInfo>> infoProducer,
            Map<Integer, TagDTO> infoMap) {
        var tagInfos = infoProducer.call();
        tagInfos.forEach(info -> {
            int id = info.getId();
            var dto = infoMap.get(id);
            if (Objects.isNull(dto)) {
                infoMap.put(id, new TagDTO());
                dto = infoMap.get(id);
            }
            int count = dto.getCount() + info.getCount();
            dto.setCount(count);
        });
    }

    public List<Tag> findMandatoryTags() {
        return tagRepository.findMandatoryTags();
    }

    public Set<Integer> getAllGroupsForTag(Integer id) {
        var tag = getById(id);
        var groupIds = new HashSet<Integer>();
        if (Objects.nonNull(tag)) {
            groupIds.add(tag.getId());
            findParentGroup(tag.getGroups(), groupIds);
        }
        return groupIds;
    }

    public <T extends TaggableEntity<? extends Number>> List<T> listByTags(List<T> entities, List<String> names) {
        return entities.stream()
                .filter(e -> e.getTags().stream()
                        .map(tag -> tag.getName().toLowerCase(Locale.ROOT))
                        .collect(Collectors.toList())
                        .containsAll(names))
                .collect(Collectors.toList());
    }

    private void findParentGroup(Set<Tag> groups, Set<Integer> groupIds) {
        groups.forEach(g -> {
            groupIds.add(g.getId());
            findParentGroup(g.getGroups(), groupIds);
        });
    }
}
