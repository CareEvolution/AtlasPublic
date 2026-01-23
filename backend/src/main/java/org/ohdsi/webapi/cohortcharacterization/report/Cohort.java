package org.ohdsi.webapi.cohortcharacterization.report;

import java.util.Objects;

public record Cohort(Integer cohortId, String cohortName) {
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Cohort(Integer id, String name)))
            return false;
        return Objects.equals(cohortId, id) &&
                Objects.equals(cohortName, name);
    }
}
