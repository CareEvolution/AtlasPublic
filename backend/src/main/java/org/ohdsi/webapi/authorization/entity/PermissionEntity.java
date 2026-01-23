package org.ohdsi.webapi.authorization.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name = "sec_permission")
public class PermissionEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1810877985769153135L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_permission_id_generator")
    @SequenceGenerator(name = "sec_permission_id_generator", sequenceName = "sec_permission_id_seq", initialValue = 500, allocationSize = 1)
    private Long id;

    @Column(name = "value")
    private String value;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "permission", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<RolePermissionEntity> rolePermissions = new LinkedHashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<RolePermissionEntity> getRolePermissions() {
        return rolePermissions;
    }

    public void setRolePermissions(Set<RolePermissionEntity> rolePermissions) {
        this.rolePermissions = rolePermissions;
    }
}
