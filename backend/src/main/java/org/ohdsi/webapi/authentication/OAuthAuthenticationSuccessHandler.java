package org.ohdsi.webapi.authentication;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.ohdsi.webapi.audittrail.events.AuditTrailSessionCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnProperty(prefix = "webapi.security", name = "mode", havingValue = "oauth")
public class OAuthAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ApplicationEventPublisher eventPublisher;
    private final CsrfTokenRepository csrfTokenRepository;

    public OAuthAuthenticationSuccessHandler(
        ApplicationEventPublisher eventPublisher,
        CsrfTokenRepository csrfTokenRepository
    ) {
        this.eventPublisher = eventPublisher;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,  HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        var redirectUrl = extractRedirectUrlFromState(request);

        var token = this.csrfTokenRepository.generateToken(request);
        this.csrfTokenRepository.saveToken(token, request, response);

        logger.info("OAuth authentication successful for user: {}", authentication.getName());
        eventPublisher.publishEvent(
            new AuditTrailSessionCreatedEvent(this, authentication.getName(), request.getSession().getId(), request.getRemoteHost()));

        response.sendRedirect(redirectUrl);
    }

    private String extractRedirectUrlFromState(HttpServletRequest request) {
        var state = request.getParameter("state");

        if (state != null) {
            // Decode the state parameter if it was URL-encoded
            var decodedState = URLDecoder.decode(state, StandardCharsets.UTF_8);

            if (decodedState.contains("redirectUrl=")) {
                var redirectUrl = decodedState
                        .substring(decodedState.indexOf("redirectUrl=") + "redirectUrl=".length());

                // Verify that redirectUrl is a valid URL or path
                if (isValidRedirectUrl(redirectUrl)) {
                    logger.debug("Valid redirect URL extracted: {}", redirectUrl);
                    return redirectUrl;
                } else {
                    logger.warn("Invalid redirect URL detected during OAuth authentication");
                    return null;
                }
            }
        }
        return null;
    }

    private boolean isValidRedirectUrl(String urlString) {
        try {
            new URI(urlString);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
