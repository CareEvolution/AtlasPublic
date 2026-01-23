package org.ohdsi.webapi.commondto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

import org.ohdsi.webapi.tag.dto.TagDTO;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class TaggableEntityDTO extends SecuredEntityDTO {
    @JsonProperty
    private Set<TagDTO> tags;

    public Set<TagDTO> getTags() {
        return tags;
    }

    public void setTags(Set<TagDTO> tags) {
        this.tags = tags;
    }
}
