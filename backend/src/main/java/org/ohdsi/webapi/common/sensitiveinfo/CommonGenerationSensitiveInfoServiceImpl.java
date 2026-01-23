package org.ohdsi.webapi.common.sensitiveinfo;

import org.ohdsi.webapi.common.generation.CommonGenerationDTO;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.springframework.stereotype.Service;

import java.util.Map;

// TODO Consider refactoring
@Service
public class CommonGenerationSensitiveInfoServiceImpl<T extends CommonGenerationDTO>
        extends AbstractSensitiveInfoService implements CommonGenerationSensitiveInfoService<T> {

    private final AuthorizationManager authorizationManager;

    public CommonGenerationSensitiveInfoServiceImpl(
            AuthorizationManager authorizationManager,
            StringResourceReader resourceReader) {
        super(resourceReader);
        this.authorizationManager = authorizationManager;
    }

    @Override
    public T filterSensitiveInfo(T generation, Map<String, Object> variables, boolean isAdmin) {
        var value = filterSensitiveInfo(generation.getExitMessage(), variables, isAdmin);
        generation.setExitMessage(value);
        return generation;
    }

    @Override
    public boolean isAdmin() {
        return authorizationManager.canReadSensitiveInfo();
    }
}
