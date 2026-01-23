/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ohdsi.webapi.conceptset;

import java.io.Serializable;
import java.util.Objects;

public class ConceptSetGenerationInfoKey implements Serializable {
    private int conceptSetId;
    private int sourceId;

    @Override
    public int hashCode() {
        return Objects.hash(conceptSetId, sourceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ConceptSetGenerationInfoKey other = (ConceptSetGenerationInfoKey) obj;
        if (this.conceptSetId != other.conceptSetId) {
            return false;
        }
        return this.sourceId == other.sourceId;
    }
}
