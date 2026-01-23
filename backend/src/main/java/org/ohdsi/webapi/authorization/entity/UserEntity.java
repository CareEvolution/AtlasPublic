package org.ohdsi.webapi.authorization.entity;

import jakarta.persistence.*;
import org.ohdsi.webapi.authorization.model.UserOrigin;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "sec_user")
public class UserEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -2697485161468660016L;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_user_id_generator")
    @SequenceGenerator(name = "sec_user_id_generator", sequenceName = "sec_user_sequence", initialValue = 1000, allocationSize = 1)
    private Long id;

    @Column(name = "login")
    private String login;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    private Set<UserRoleEntity> userRoles = new LinkedHashSet<>();

    @Column(name = "last_viewed_notifications_time")
    private Date lastViewedNotificationsTime;

    @Column(name = "origin", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserOrigin origin = UserOrigin.SYSTEM;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<UserRoleEntity> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Set<UserRoleEntity> userRoles) {
        this.userRoles = userRoles;
    }

    public Date getLastViewedNotificationsTime() {
        return lastViewedNotificationsTime;
    }

    public void setLastViewedNotificationsTime(Date lastViewedNotificationsTime) {
        this.lastViewedNotificationsTime = lastViewedNotificationsTime;
    }

    public UserOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(UserOrigin origin) {
        this.origin = origin;
    }
}
