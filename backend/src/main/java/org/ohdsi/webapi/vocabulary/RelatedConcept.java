package org.ohdsi.webapi.vocabulary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ohdsi.vocabulary.Concept;

import java.util.ArrayList;

@JsonInclude(Include.NON_NULL)
public class RelatedConcept extends Concept {
    @JsonProperty("RELATIONSHIPS")
    public ArrayList<ConceptRelationship> relationships;

    public RelatedConcept() {
        relationships = new ArrayList<>();
    }

    @JsonProperty("RELATIONSHIP_CAPTION")
    public String GetRelationshipCaption() {
        var result = new StringBuilder();
        for (var i = 0; i < relationships.size(); i++) {
            if (i > 0)
                result.append(", ");
            result.append(relationships.get(i).relationshipName);
        }
        return result.toString();
    }
}
