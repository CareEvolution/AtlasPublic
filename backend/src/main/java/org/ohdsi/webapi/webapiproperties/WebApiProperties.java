package org.ohdsi.webapi.webapiproperties;

import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@Validated
//@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix="webapi")
public class WebApiProperties {
    @Valid
    @NestedConfigurationProperty
    private AppDbProperties appDb = new AppDbProperties();
    public AppDbProperties getAppDb() {
        return appDb;
    }
    public AppDbProperties appDb() {
        return this.appDb;
    }
    public void setAppDb(AppDbProperties appDb) {
        this.appDb = appDb;
    }

    @Valid
    @NestedConfigurationProperty
    private AuditTrailProperties auditTrail = new AuditTrailProperties();
    public AuditTrailProperties getAuditTrail() {
        return auditTrail;
    }
    public AuditTrailProperties auditTrail() {
        return auditTrail;
    }
    public void setAuditTrail(AuditTrailProperties auditTrail) {
        this.auditTrail = auditTrail;
    }

    @Valid
    @NestedConfigurationProperty
    private CdmDbProperties cdmDb = new CdmDbProperties();
    public CdmDbProperties getCdmDb() {
        return cdmDb;
    }
    public CdmDbProperties cdmDb() {
        return cdmDb;
    }
    public void setCdmDb(CdmDbProperties cdmDb) {
        this.cdmDb = cdmDb;
    }

    @Valid
    @NestedConfigurationProperty
    private CdmResultsProperties cdmResults = new CdmResultsProperties();
    public CdmResultsProperties getCdmResults() {
        return cdmResults;
    }
    public CdmResultsProperties cdmResults() {
        return cdmResults;
    }
    public void setCdmResults(CdmResultsProperties cdmResults) {
        this.cdmResults = cdmResults;
    }

    @Valid
    @NestedConfigurationProperty
    private CharacterizationProperties characterization = new CharacterizationProperties();

    public CharacterizationProperties getCharacterization() {
        return characterization;
    }

    public CharacterizationProperties characterization() {
        return characterization;
    }

    public void setCharacterization(CharacterizationProperties characterization) {
        this.characterization = characterization;
    }

    @Valid
    @NestedConfigurationProperty
    private GenerationCacheProperties generationCache = new GenerationCacheProperties();
    public GenerationCacheProperties getGenerationCache() {
        return generationCache;
    }
    public GenerationCacheProperties generationCache() {
        return generationCache;
    }
    public void setGenerationCache(GenerationCacheProperties generationCache) {
        this.generationCache = generationCache;
    }

    @Valid
    @NestedConfigurationProperty
    private HeraclesProperties heracles = new HeraclesProperties();
    public HeraclesProperties getHeracles() {
        return heracles;
    }
    public HeraclesProperties heracles() {
        return heracles;
    }
    public void setHeracles(HeraclesProperties heracles) {
        this.heracles = heracles;
    }

    @Valid
    @NestedConfigurationProperty
    private I18nProperties i18n = new I18nProperties();
    public I18nProperties getI18n() {
        return i18n;
    }
    public I18nProperties i18n() {
        return i18n;
    }
    public void setI18n(I18nProperties i18n) {
        this.i18n = i18n;
    }

    @Valid
    @NestedConfigurationProperty
    private PersonProperties person = new PersonProperties();
    public PersonProperties getPerson() {
        return person;
    }
    public PersonProperties person() {
        return person;
    }
    public void setPerson(PersonProperties person) {
        this.person = person;
    }

    @Valid
    @NestedConfigurationProperty
    private WebApiSecurityProperties security = new WebApiSecurityProperties();
    public WebApiSecurityProperties getSecurity() {
        return security;
    }
    public WebApiSecurityProperties security() {
        return security;
    }
    public void setSecurity(WebApiSecurityProperties security) {
        this.security = security;
    }

    @Valid
    @NestedConfigurationProperty
    private TagProperties tag = new TagProperties();
    public TagProperties getTag() {
        return tag;
    }
    public TagProperties tag() {
        return tag;
    }
    public void setTag(TagProperties tag) {
        this.tag = tag;
    }

    @Valid
    @NestedConfigurationProperty
    private UserProperties user = new UserProperties();
    public UserProperties getUser() {
        return user;
    }
    public UserProperties user() {
        return user;
    }
    public void setUser(UserProperties user) {
        this.user = user;
    }
}