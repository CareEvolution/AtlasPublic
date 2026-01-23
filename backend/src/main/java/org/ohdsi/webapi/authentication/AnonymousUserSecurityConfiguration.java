package org.ohdsi.webapi.authentication;

import java.util.Arrays;

import org.ohdsi.webapi.authorization.AnonymousUser;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration
@ConditionalOnProperty(name = "webapi.security.mode", havingValue = "anonymous")
public class AnonymousUserSecurityConfiguration {

    private static SimpleGrantedAuthority[] anonymous_authorities;
    private final CsrfTokenRepository csrfTokenRepository;

    public AnonymousUserSecurityConfiguration(AuthorizationManager authorizationManager, CsrfTokenRepository csrfTokenRepository) {
        var rawPermissions = authorizationManager.getRawPermissions(AnonymousUser.NAME);
        anonymous_authorities = rawPermissions.stream().filter(p -> p != null && !p.isBlank())
                .map(SimpleGrantedAuthority::new)
                .toArray(SimpleGrantedAuthority[]::new);
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                .ignoringRequestMatchers(SecurityConfiguration.NO_AUTH_NEEDED_ROUTES)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(SecurityConfiguration.AUTH_ALWAYS_NEEDED_ROUTES).denyAll() // Deny access to forbidden resources
                .anyRequest().permitAll() // Allow access to all other requests
            )
            .anonymous(anonymous -> anonymous
                .authorities(Arrays.asList(anonymous_authorities))
            );

        return http.build();
    }
}