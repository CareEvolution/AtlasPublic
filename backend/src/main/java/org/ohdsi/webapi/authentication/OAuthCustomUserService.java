package org.ohdsi.webapi.authentication;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class OAuthCustomUserService extends OidcUserService {

    private final AuthorizationManager authorizationManager;

    public OAuthCustomUserService(AuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);
        var login = oidcUser.getEmail();
        var name = oidcUser.getFullName();

        if (!authorizationManager.userExists(login)) {
            authorizationManager.registerNewUser(login, StringUtils.defaultIfBlank(name, login));
        }

        Set<GrantedAuthority> authorities = new HashSet<>(oidcUser.getAuthorities());
        var rawPermissions = authorizationManager.getRawPermissions(login);
        for (var permission : rawPermissions) {
            if (StringUtils.isNotBlank(permission)) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }
        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "email");
    }
}
