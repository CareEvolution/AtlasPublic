package org.ohdsi.webapi.authentication;

import org.ohdsi.webapi.authorization.event.SuccessLogoutEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration
@ConditionalOnProperty(prefix = "webapi.security", name = "mode", havingValue = "database")
public class DatabaseSecurityConfiguration {
    private final DatabaseUserDetailsService databaseUserDetailsService;
    private final CsrfTokenRepository csrfTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DatabaseSecurityConfiguration(
            DatabaseUserDetailsService databaseUserDetailsService,
            CsrfTokenRepository csrfTokenRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.databaseUserDetailsService = databaseUserDetailsService;
        this.csrfTokenRepository = csrfTokenRepository;
        this.eventPublisher = eventPublisher;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return databaseUserDetailsService;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authenticationProvider);
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
            .logout((logout) -> logout
                .logoutUrl("/user/logout") 
                .addLogoutHandler(databaseLogoutHandler())
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
            );
            
        return http.build();
    }

    @Bean
    public LogoutHandler databaseLogoutHandler() {
        return (request, response, authentication) -> {
            eventPublisher.publishEvent(new SuccessLogoutEvent(this, authentication.getName()));
            SecurityContextHolder.clearContext();
        };
    }

}
