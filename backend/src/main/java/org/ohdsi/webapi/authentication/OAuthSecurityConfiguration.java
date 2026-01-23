package org.ohdsi.webapi.authentication;

import org.ohdsi.webapi.audittrail.events.AuditTrailLoginFailedEvent;
import org.ohdsi.webapi.authorization.event.SuccessLogoutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration
@ConditionalOnProperty(prefix = "webapi.security", name = "mode", havingValue = "oauth")
public class OAuthSecurityConfiguration {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final OAuthAuthenticationSuccessHandler authSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CsrfTokenRepository csrfTokenRepository;
    private final OAuthCustomUserService oAuthCustomUserService;

    public OAuthSecurityConfiguration(
        CsrfTokenRepository csrfTokenRepository,
        OAuthAuthenticationSuccessHandler authSuccessHandler, 
        ClientRegistrationRepository clientRegistrationRepository,
        ApplicationEventPublisher eventPublisher,
        OAuthCustomUserService oAuthCustomUserService
    ) {
        this.csrfTokenRepository = csrfTokenRepository;
        this.authSuccessHandler = authSuccessHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.eventPublisher = eventPublisher;
        this.oAuthCustomUserService = oAuthCustomUserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                .ignoringRequestMatchers(SecurityConfiguration.NO_AUTH_NEEDED_ROUTES)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(SecurityConfiguration.NO_AUTH_NEEDED_ROUTES).permitAll() // Allow access to public resources
                .anyRequest().authenticated() // Require authentication for all other requests
            )
            .oauth2Login(login -> login
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestResolver(new OAuth2CustomAuthorizationRequestResolver(
                        clientRegistrationRepository,
                        "/oauth2/authorization"
                    ))
                )
                .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oAuthCustomUserService))
                .successHandler(authSuccessHandler)
                .failureHandler(oauthAuthenticationFailureHandler())
            )
            .exceptionHandling(e -> e
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .logout(logout -> logout
                .logoutUrl("/user/logout") 
                .addLogoutHandler(oauthLogoutHandler())
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
            )
            .cors(Customizer.withDefaults());
            
        return http.build();
    }
    
    @Bean
    public LogoutHandler oauthLogoutHandler() {
        return (request, response, authentication) -> {
            SecurityContextHolder.clearContext();
            eventPublisher.publishEvent(new SuccessLogoutEvent(this, authentication.getName()));
        };
    }

    @Bean
    public AuthenticationFailureHandler oauthAuthenticationFailureHandler() {
        return (request, response, exception) -> {
            logger.error("OAuth authentication failed: {}", exception);
            eventPublisher.publishEvent(new AuditTrailLoginFailedEvent(this, request.getParameter("username"), request.getRemoteHost()));
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        };
    }
}
