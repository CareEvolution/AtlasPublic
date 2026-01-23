package org.ohdsi.webapi.common.sensitiveinfo;

import org.ohdsi.webapi.cohortdefinition.CohortGenerationInfo;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.ohdsi.webapi.authorization.AuthorizationManager;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CohortGenerationSensitiveInfoServiceImpl extends AbstractSensitiveInfoService
        implements CohortGenerationSensitiveInfoService {

    private final AuthorizationManager authorizationManager;

    public CohortGenerationSensitiveInfoServiceImpl(
            AuthorizationManager authorizationManager,
            StringResourceReader resourceReader) {
        super(resourceReader);
        this.authorizationManager = authorizationManager;
    }

    @Override
    public CohortGenerationInfo filterSensitiveInfo(CohortGenerationInfo source, Map<String, Object> variables,
            boolean isAdmin) {
        String value = filterSensitiveInfo(source.getFailMessage(), variables, isAdmin);
        source.setFailMessage(value);
        return source;
    }

    @Override
    public boolean isAdmin() {
        return authorizationManager.canReadSensitiveInfo();
    }
}
