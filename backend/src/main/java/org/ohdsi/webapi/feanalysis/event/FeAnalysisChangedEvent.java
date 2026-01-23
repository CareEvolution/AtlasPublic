package org.ohdsi.webapi.feanalysis.event;

import org.ohdsi.webapi.feanalysis.domain.FeAnalysisEntity;

// TODO fix raw use of parameterized class FeAnalysisEntity
public class FeAnalysisChangedEvent {

    private FeAnalysisEntity feAnalysis;

    public FeAnalysisChangedEvent(FeAnalysisEntity feAnalysis) {
        this.feAnalysis = feAnalysis;
    }

    public FeAnalysisEntity getFeAnalysis() {
        return feAnalysis;
    }
}
