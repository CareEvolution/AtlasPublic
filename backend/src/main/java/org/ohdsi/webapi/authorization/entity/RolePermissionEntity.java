package org.ohdsi.webapi.authorization.entity;

import java.io.Serial;
import java.io.Serializable;

import jakarta.persistence.*;

@Entity
@Table(name = "sec_role_permission")
public class RolePermissionEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 6257846375334314942L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_role_permission_id_generator")
    @SequenceGenerator(name = "sec_role_permission_id_generator", sequenceName = "sec_role_permission_sequence", initialValue = 1000, allocationSize = 1)
    private Long id;

    @Column(name = "status")
    private String status;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionEntity permission;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public PermissionEntity getPermission() {
        return permission;
    }

    public void setPermission(PermissionEntity permission) {
        this.permission = permission;
    }
}
