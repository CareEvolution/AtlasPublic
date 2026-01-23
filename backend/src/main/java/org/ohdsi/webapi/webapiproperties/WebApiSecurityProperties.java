package org.ohdsi.webapi.webapiproperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class WebApiSecurityProperties {
    @Valid
    @NestedConfigurationProperty
    private AuthDbProperties authDb = new AuthDbProperties();

    @Valid
    @NestedConfigurationProperty
    private CorsProperties cors = new CorsProperties();

    @NotNull(message="security.default-global-read-permissions must have a value")
    private Boolean defaultGlobalReadPermissions;

    @NotNull(message="security.encryption-key must have a value")
    private String encryptionKey;

    @NotBlank(message="security.origin must have a value")
    private String origin;

    @Pattern(regexp = "anonymous|database|oauth", message = "invalid security.mode value")
    private String mode;

    @NotNull(message="security.token-expiration-seconds must have a value")
    private Long tokenExpirationSeconds;

    public AuthDbProperties getAuthDb() {
        return authDb;
    }
    public AuthDbProperties authDb() {
        return authDb;
    }
    public void setAuthDb(AuthDbProperties authDb) {
        this.authDb = authDb;
    }

    public CorsProperties getCors() {
        return cors;
    }
    public CorsProperties cors() {
        return cors;
    }
    public void setCors(CorsProperties cors) {
        this.cors = cors;
    }

    public Boolean getDefaultGlobalReadPermissions() {
        return defaultGlobalReadPermissions;
    }
    public Boolean defaultGlobalReadPermissions() {
        return defaultGlobalReadPermissions;
    }
    public void setDefaultGlobalReadPermissions(Boolean defaultGlobalReadPermissions) {
        this.defaultGlobalReadPermissions = defaultGlobalReadPermissions;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }
    public String encryptionKey() {
        return encryptionKey;
    }
    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getOrigin() {
        return origin;
    }
    public String origin() {
        return origin;
    }
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getMode() {
        return mode;
    }

    public String mode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Long getTokenExpirationSeconds() {
        return tokenExpirationSeconds;
    }

    public void setTokenExpirationSeconds(Long tokenExpirationSeconds) {
        this.tokenExpirationSeconds = tokenExpirationSeconds;
    }

    public Long tokenExpirationSeconds() {
        return tokenExpirationSeconds;
    }
}
