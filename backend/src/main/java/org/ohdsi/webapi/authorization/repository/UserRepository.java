package org.ohdsi.webapi.authorization.repository;

import java.util.List;
import java.util.Set;

import org.ohdsi.webapi.authorization.model.UserOrigin;
import org.ohdsi.webapi.authorization.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    public UserEntity findByLogin(String login);

    @Query("SELECT u.login FROM UserEntity u")
    public Set<String> getUserLogins();

    @Query("from UserEntity where login = 'testLogin'")
    public UserEntity getTestUser();

    List<UserEntity> findByOrigin(UserOrigin origin);

    //@Query("select u from UserEntity u JOIN FETCH u.userRoles ")
    //List<UserEntity> findAllWithChildrenEagerly();
}
