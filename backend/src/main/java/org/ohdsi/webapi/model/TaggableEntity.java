package org.ohdsi.webapi.model;

import org.ohdsi.webapi.tag.domain.Tag;

import jakarta.persistence.MappedSuperclass;
import java.util.Set;

@MappedSuperclass
public abstract class TaggableEntity<T extends Number> extends SecuredEntity<T> {
    public abstract Set<Tag> getTags();

    public abstract void setTags(Set<Tag> tags);
}
