package org.ohdsi.webapi.util;

import org.ohdsi.webapi.webapiproperties.WebApiProperties;
import org.springframework.stereotype.Service;

// This class exists to initialize the static encryptionKey field in EncryptionUtils with the value from WebApiProperties.
// Why do we need to do this in this way, you ask? The EncryptionUtils are used by the SecretAttributeConverter,
// which is a JPA AttributeConverter. JPA manually creates instances of the converter so dependency injection does
// not occur. Also, that class requires a default parameterless constructor, so we can't inject the encryption utilities,
// into it, nor the encryption key. So this is a hack around these limitations. In order for Spring Boot to locate
// this component, it cannot be nested inside of the EncryptionUtils class.

@Service
public class EncryptionUtilsInitializer {
    public EncryptionUtilsInitializer(WebApiProperties webApiProperties) {
        EncryptionUtils.encryptionKey = webApiProperties.getSecurity().getEncryptionKey();
    }
}
