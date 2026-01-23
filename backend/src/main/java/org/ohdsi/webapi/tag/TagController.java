package org.ohdsi.webapi.tag;

import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.authorization.model.SecuredEntityType;
import org.ohdsi.webapi.tag.converter.TagDtoToTag;
import org.ohdsi.webapi.tag.domain.Tag;
import org.ohdsi.webapi.tag.dto.TagDTO;
import org.ohdsi.webapi.tag.dto.TagGroupSubscriptionDTO;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for managing tags.
 */
@RestController
@RequestMapping("/tag")
public class TagController {
    private final TagService tagService;
    private final TagGroupService tagGroupService;
    private final ConversionService conversionService;
    private final AuthorizationManager authorizationManager;

    public TagController(TagService tagService,
            TagGroupService tagGroupService,
            ConversionService conversionService,
            AuthorizationManager authorizationManager
    ) {
        this.tagService = tagService;
        this.tagGroupService = tagGroupService;
        this.conversionService = conversionService;
        this.authorizationManager = authorizationManager;
    }

    /**
     * Creates a tag.
     *
     * @param dto
     * @return the created tag
     */

    @PostMapping("")
    @Transactional
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.CREATE_TAG)")
    public TagDTO create(@RequestBody final TagDTO dto) {
        var tag = TagDtoToTag.convert(dto);
        var saved = tagService.create(tag);
        return convertToDTO(saved);
    }

    /**
     * Returns list of tags, which names contain a provided substring.
     *
     * @param namePart
     * @return a list of tags
     */
    @GetMapping("/search")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_TAG)")
    public List<TagDTO> search(@RequestParam("namePart") String namePart) {
        if (!StringUtils.hasText(namePart)) {
            return Collections.emptyList();
        }
        return tagService
            .listInfo(namePart)
            .stream()
            .filter(tag -> authorizationManager.currentUserCanRead(tag))
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Returns list of all tags.
     *
     * @return a list of tags
     */
    @GetMapping("")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_TAG)")
    public List<TagDTO> list() {
        return tagService
            .listInfo()
            .stream()
            .filter(tag -> authorizationManager.currentUserCanRead(tag))
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Updates tag with ID={id}.
     *
     * @param id
     * @param dto
     * @return the updated tag
     */
    @PutMapping("/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.UPDATE_TAG)")
    @Transactional
    public TagDTO update(@PathVariable("id") final Integer id, @RequestBody final TagDTO dto) {
        if (!authorizationManager.currentUserCanUpdate(SecuredEntityType.TAG, id)) {
            throw new AccessDeniedException("You do not have permission to update this tag");
        }
        var toUpdate = TagDtoToTag.convert(dto);
        var updated = tagService.update(id, toUpdate);
        return convertToDTO(updated);
    }

    /**
     * Return tag by ID.
     *
     * @param id
     * @return the tag with the given ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.READ_TAG)")
    public TagDTO get(@PathVariable("id") final Integer id) {
        if (!authorizationManager.currentUserCanRead(SecuredEntityType.TAG, id)) {
            throw new AccessDeniedException("You do not have permission to read this tag");
        }
        var tag = tagService.getById(id);
        return convertToDTO(tag);
    }

    /**
     * Deletes tag with ID={id}.
     *
     * @param id
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@authorizationManager.currentUserHasPermission(@PERMS.DELETE_TAG)")
    @Transactional
    public void delete(@PathVariable("id") final Integer id) {
        if (!authorizationManager.currentUserCanDelete(SecuredEntityType.TAG, id)) {
            throw new AccessDeniedException("You do not have permission to delete this tag");
        }
        tagService.delete(id);
    }

    /**
     * Assigns group of tags to groups of assets.
     *
     * @param dto
     *        A group of tags and a group of assets to assign those tags to
     */

    @PostMapping("/multiAssign")
    @Transactional
    public void assignGroup(@RequestBody final TagGroupSubscriptionDTO dto) {
        checkMultiAssignPermissions(dto);
        tagGroupService.assignGroup(dto);
    }

    /**
     * Unassignes group of tags from groups of assets.
     *
     * @param dto
     *       A group of tags and a group of assets to unassign those tags from
     */
    @PostMapping("/multiUnassign")
    @Transactional
    public void unassignGroup(@RequestBody final TagGroupSubscriptionDTO dto) {
        checkMultiAssignPermissions(dto);
        tagGroupService.unassignGroup(dto);
    }

    private TagDTO convertToDTO(Tag tag) {
        var tagDTO = conversionService.convert(tag, TagDTO.class);
        authorizationManager.fillPermissions(tag, tagDTO);
        return tagDTO;
    }

    private void checkMultiAssignPermissions(TagGroupSubscriptionDTO dto) {
        if (!dto.getAssets().characterizations().stream().allMatch(
                id -> authorizationManager.currentUserCanUpdate(SecuredEntityType.COHORT_CHARACTERIZATION, id))) {
            throw new AccessDeniedException(
                    "You do not have permission to assign tags to one or more cohort characterizations");
        }
        if (!dto.getAssets().cohorts().stream().allMatch(
                id -> authorizationManager.currentUserCanUpdate(SecuredEntityType.COHORT_DEFINITION, id))) {
            throw new AccessDeniedException(
                    "You do not have permission to assign tags to one or more cohort definitions");
        }
        if (!dto.getAssets().conceptSets().stream().allMatch(
                id -> authorizationManager.currentUserCanUpdate(SecuredEntityType.CONCEPT_SET, id))) {
            throw new AccessDeniedException(
                    "You do not have permission to assign tags to one or more concept sets");
        }
    }
}
