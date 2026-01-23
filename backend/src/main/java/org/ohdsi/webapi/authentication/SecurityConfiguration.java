package org.ohdsi.webapi.authentication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    private final WebApiProperties webApiProperties;
    private final Optional<ClientRegistrationRepository> clientRegistrationRepository;

    public SecurityConfiguration(WebApiProperties webApiProperties, Optional<ClientRegistrationRepository> clientRegistrationRepository) {
        this.webApiProperties = webApiProperties;
        this.clientRegistrationRepository = clientRegistrationRepository;

        List<String> routesList = new ArrayList<>(Arrays.asList(NO_AUTH_NEEDED_ROUTES));
        clientRegistrationRepository.ifPresent((repo) -> {
            ((InMemoryClientRegistrationRepository) repo).forEach((clientRegistration) -> {
                routesList.add("/oauth2/authorization/" + clientRegistration.getRegistrationId());
                routesList.add("/login/oauth2/code/" + clientRegistration.getRegistrationId());
            });
            NO_AUTH_NEEDED_ROUTES = routesList.toArray(new String[0]);
        });
    }

    public static String[] NO_AUTH_NEEDED_ROUTES = {
        "/i18n/**",
        "/actuator/info",
        "/user/login/db",
    };

    public static final String[] AUTH_ALWAYS_NEEDED_ROUTES = {
        // "/permission/**",
        // "/role/**",
        // "/user/**"
    };

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource() {
        var allowedOrigins = webApiProperties.getSecurity().getCors().getAllowedOrigins();
        List<String> allowedOriginsList = new ArrayList<>(Arrays.asList(allowedOrigins));
        clientRegistrationRepository.ifPresent((repo) -> {
            ((InMemoryClientRegistrationRepository) repo).forEach((clientRegistration) -> {
                allowedOriginsList.add(clientRegistration.getProviderDetails().getIssuerUri());
            });
        });

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginsList);
        configuration.setAllowedMethods(List.of("GET","POST", "PUT","DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",configuration);
        return source;
    }

}
