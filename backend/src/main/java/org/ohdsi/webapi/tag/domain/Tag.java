package org.ohdsi.webapi.tag.domain;

import jakarta.persistence.*;
import org.ohdsi.webapi.model.SecuredEntity;
import org.ohdsi.webapi.tag.converter.TagTypeConverter;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "tag")
public class Tag extends SecuredEntity<Integer> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tag_id_generator")
    @SequenceGenerator(name = "tag_id_generator", sequenceName = "tag_seq", allocationSize = 1)
    private Integer id;

    @ManyToMany(targetEntity = Tag.class, fetch = FetchType.LAZY)
    @JoinTable(name = "tag_group",
            joinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "group_id", referencedColumnName = "id"))
    private Set<Tag> groups;

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    @Convert(converter = TagTypeConverter.class)
    private TagType type;

    @Column(name = "count")
    private int count;

    @Column(name = "show_group")
    private boolean showGroup;

    @Column(name = "multi_selection")
    private boolean multiSelection;

    @Column(name = "permission_protected")
    private boolean permissionProtected;

    @Column(name = "icon")
    private String icon;

    @Column(name = "color")
    private String color;

    @Column(name = "mandatory")
    private boolean mandatory;

    @Column(name = "allow_custom")
    private boolean allowCustom;

    @Column(name = "description")
    private String description;

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Set<Tag> getGroups() {
        return groups;
    }

    public void setGroups(Set<Tag> groups) {
        this.groups = groups;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TagType getType() {
        return type;
    }

    public void setType(TagType type) {
        this.type = type;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isShowGroup() {
        return showGroup;
    }

    public void setShowGroup(boolean showGroup) {
        this.showGroup = showGroup;
    }

    public boolean isMultiSelection() {
        return multiSelection;
    }

    public void setMultiSelection(boolean multiSelection) {
        this.multiSelection = multiSelection;
    }

    public boolean isPermissionProtected() {
        return permissionProtected;
    }

    public void setPermissionProtected(boolean permissionProtected) {
        this.permissionProtected = permissionProtected;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isAllowCustom() {
        return allowCustom;
    }

    public void setAllowCustom(boolean allowCustom) {
        this.allowCustom = allowCustom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Tag tag = (Tag) o;
        return name.equals(tag.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
