/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ohdsi.webapi.conceptset;

import org.ohdsi.circe.vocabulary.ConceptSetExpression;

public class ConceptSetOptimizationResult {
    public ConceptSetExpression optimizedConceptSet;
    public ConceptSetExpression removedConceptSet;

    public ConceptSetOptimizationResult() {
        this.optimizedConceptSet = new ConceptSetExpression();
        this.removedConceptSet = new ConceptSetExpression();
    }
}
