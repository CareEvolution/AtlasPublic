package org.ohdsi.webapi.authorization.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name = "sec_role")
public class RoleEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 6257846375334314942L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_role_id_generator")
    @SequenceGenerator(name = "sec_role_id_generator", sequenceName = "sec_role_sequence", initialValue = 1000, allocationSize = 1)
    private Long id;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<RolePermissionEntity> rolePermissions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<UserRoleEntity> userRoles = new LinkedHashSet<>();

    @Column(name = "system_role")
    private Boolean systemRole;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<RolePermissionEntity> getRolePermissions() {
        return rolePermissions;
    }

    public void setRolePermissions(Set<RolePermissionEntity> rolePermissions) {
        this.rolePermissions = rolePermissions;
    }

    public Set<UserRoleEntity> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Set<UserRoleEntity> userRoles) {
        this.userRoles = userRoles;
    }

    public Boolean isSystemRole() {
        return systemRole;
    }

    public void setSystemRole(Boolean system) {
        systemRole = system;
    }
}
