package org.ohdsi.webapi.authentication;

import org.ohdsi.webapi.authentication.dto.LoginDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * REST API for handling user authentication when using database authentication mode. This controller is only active when the application property "webapi.security.mode" is set to "database".
 */
@Component
@RestController
@ConditionalOnProperty(prefix = "webapi.security", name = "mode", havingValue = "database")
public class DatabaseLoginController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public DatabaseLoginController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Authenticate the user using the provided login credentials.
     * @param loginDto
     *      The login credentials containing the username and password.
     * @param request
     * @param response
     * @return
     *     A ResponseEntity indicating the success or failure of the authentication process.
     */
    @PostMapping("/user/login/db")
    public ResponseEntity<Void> authenticateUser(@RequestBody LoginDTO loginDto, HttpServletRequest request, HttpServletResponse response) {

        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(loginDto.login(),
                loginDto.password());
        Authentication authenticationResponse = this.authenticationManager.authenticate(authenticationRequest);

        // Set the Authentication in the SecurityContextHolder
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticationResponse);
        SecurityContextHolder.setContext(context);

        // Save the SecurityContext to the SecurityContextRepository
        securityContextRepository.saveContext(context, request, response);

        return ResponseEntity.ok().build();
    }
}
