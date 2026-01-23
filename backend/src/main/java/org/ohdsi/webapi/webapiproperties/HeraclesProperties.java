package org.ohdsi.webapi.webapiproperties;

import jakarta.validation.constraints.PositiveOrZero;

public class HeraclesProperties {
    @PositiveOrZero(message="webapi.heracles.small-cell-count must be >= 0")
    private Integer smallCellCount;

    public Integer getSmallCellCount() {
        return smallCellCount;
    }
    public Integer smallCellCount() {
        return smallCellCount;
    }
    public void setSmallCellCount(Integer smallCellCount) {
        this.smallCellCount = smallCellCount;
    }
}
