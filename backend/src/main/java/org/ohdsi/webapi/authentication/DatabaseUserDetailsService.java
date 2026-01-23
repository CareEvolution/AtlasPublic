package org.ohdsi.webapi.authentication;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty(prefix = "webapi.security", name = "mode", havingValue = "database")
public class DatabaseUserDetailsService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(DatabaseUserDetailsService.class);

    private final DataSource dataSource;
    private final WebApiProperties webApiProperties;
    private final AuthorizationManager authorizationManager;


    public DatabaseUserDetailsService(
        @Qualifier("AuthDbDataSource") DataSource dataSource, 
        WebApiProperties webApiProperties,
        AuthorizationManager authorizationManager
    ) {
        this.dataSource = dataSource;
        this.webApiProperties = webApiProperties;
        this.authorizationManager = authorizationManager;
    }
    

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        try (
                Connection conn = dataSource.getConnection();
                PreparedStatement ps = createPreparedStatement(conn, username);
                ResultSet rs = ps.executeQuery()
        ) {
            if (!rs.next()) {
                throw new UsernameNotFoundException("User [" + username + "] not found.");
            }
            var userBuilder = extractUserDetails(rs, username);

            if (!authorizationManager.userExists(username)) {
                authorizationManager.registerNewUser(username, username);
            }

            var rawPermissions = authorizationManager.getRawPermissions(username);
            userBuilder.authorities(rawPermissions.stream().filter(StringUtils::isNotBlank).toArray(String[]::new));

            return userBuilder.build();
        } catch (SQLException e) {
            String message = "There was a SQL error while authenticating user [" + username + "]";
            log.error(message, e);
        }
        return null;
    }

    private PreparedStatement createPreparedStatement(Connection conn, String username) throws SQLException {
        String schemaName = webApiProperties.getSecurity().getAuthDb().getSchemaName();
        String authenticationQuery = String.format(
            "select password,firstName,middleName,lastName from %s.users where username = ?",
            schemaName
        );
        PreparedStatement ps = conn.prepareStatement(authenticationQuery);
        ps.setString(1, username);
        return ps;
    }

    private UserBuilder extractUserDetails(ResultSet rs, String username) throws SQLException {

        var userBuilder = User.builder()
                .username(username)
                .password(rs.getString("password"));
        return userBuilder;
    }
}
