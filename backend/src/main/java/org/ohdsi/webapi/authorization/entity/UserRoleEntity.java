package org.ohdsi.webapi.authorization.entity;

import java.io.Serial;
import java.io.Serializable;

import jakarta.persistence.*;
import org.ohdsi.webapi.authorization.model.UserOrigin;

@Entity
@Table(name = "sec_user_role")
public class UserRoleEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 6257846375334314942L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_user_role_id_generator")
    @SequenceGenerator(name = "sec_user_role_id_generator", sequenceName = "sec_user_role_sequence", initialValue = 1000, allocationSize = 1)
    private Long id;

    @Column(name = "status")
    private String status;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(name = "origin", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserOrigin origin = UserOrigin.SYSTEM;

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

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public RoleEntity getRole() {
        return role;
    }

    public void setRole(RoleEntity role) {
        this.role = role;
    }

    public UserOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(UserOrigin origin) {
        this.origin = origin;
    }
}
