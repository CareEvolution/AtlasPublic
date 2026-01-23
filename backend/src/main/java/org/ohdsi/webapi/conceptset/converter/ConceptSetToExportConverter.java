package org.ohdsi.webapi.conceptset.converter;

import org.ohdsi.circe.cohortdefinition.ConceptSet;
import org.ohdsi.webapi.conceptset.ConceptSetExport;
public class ConceptSetToExportConverter {
    public static ConceptSetExport convert(ConceptSet conceptSet) {
        var export = new ConceptSetExport();
        export.ConceptSetId = conceptSet.id;
        export.ConceptSetName = conceptSet.name;
        export.csExpression = conceptSet.expression;
        return export;
    }
}
