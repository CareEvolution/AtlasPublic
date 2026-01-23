package org.ohdsi.webapi.cohortcharacterization.report;

import java.util.Set;

public class AnalysisResultItem {
    private final Set<String> domainIds;
    private final Set<Cohort> cohorts;
    private final Set<ExportItem<?>> exportItems;

    public AnalysisResultItem(Set<String> domainIds, Set<Cohort> cohorts,
            Set<ExportItem<?>> exportItems) {
        this.domainIds = domainIds;
        this.cohorts = cohorts;
        this.exportItems = exportItems;
    }

    public Set<String> getDomainIds() {
        return domainIds;
    }

    public Set<Cohort> getCohorts() {
        return cohorts;
    }

    public Set<ExportItem<?>> getExportItems() {
        return exportItems;
    }
}
