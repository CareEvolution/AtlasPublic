package org.ohdsi.webapi.feanalysis;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ohdsi.analysis.cohortcharacterization.design.CcResultType;
import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisDomain;
import org.ohdsi.analysis.cohortcharacterization.design.StandardFeatureAnalysisType;
import org.ohdsi.circe.cohortdefinition.ConceptSet;
import org.ohdsi.webapi.feanalysis.dto.BaseFeAnalysisCriteriaDTO;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisDTO;
import org.ohdsi.webapi.feanalysis.dto.FeAnalysisWithConceptSetDTO;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FeAnalysisDeserializer extends JsonDeserializer<FeAnalysisDTO> {

    // NOTE: This needs to be autowired because we need a parameterless constructor for this class
    @Autowired
    private ObjectMapper objectMapper;

    // need to look around and find a way to override procedure of base mapping
    // and handle only a design field
    @Override
    public FeAnalysisDTO deserialize(final JsonParser parser, final DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);
        var dto = createDto(node);

        final var name = node.get("name");
        if (name != null && !name.isNull()) {
            dto.setName(name.textValue());
        }

        final var description = node.get("description");
        if (description != null && !description.isNull() && name != null && !name.isNull()) {
            dto.setDescription(description.textValue());
        }

        final var descr = node.get("descr");
        if (descr != null && !descr.isNull()) {
            dto.setDescription(descr.textValue());
        }

        final var id = node.get("id");
        if (id != null && !id.isNull()) {
            dto.setId(id.intValue());
        }

        final var domain = node.get("domain");
        if (domain != null && !domain.isNull()) {
            final var domainString = domain.textValue();
            dto.setDomain(StandardFeatureAnalysisDomain.valueOf(domainString));
        }

        final var analysisType = getType(node);
        if (analysisType != null) {
            dto.setType(analysisType);

            final var design = node.get("design");
            if (analysisType == StandardFeatureAnalysisType.CRITERIA_SET) {
                var statType = node.get("statType");
                if (statType != null) {
                    dto.setStatType(CcResultType.valueOf(statType.textValue()));
                }
                final var list = new ArrayList<BaseFeAnalysisCriteriaDTO>();
                for (final var jsonNode : design) {
                    list.add(convert(jsonNode));
                }
                dto.setDesign(list);

                final var conceptSets = node.get("conceptSets");
                if (Objects.nonNull(conceptSets)) {
                    var typeRef = objectMapper.getTypeFactory().constructCollectionType(List.class,
                            ConceptSet.class);
                    List<ConceptSet> conceptSetList = objectMapper.readValue(conceptSets.traverse(), typeRef);
                    ((FeAnalysisWithConceptSetDTO) dto).setConceptSets(conceptSetList);
                }
            } else {
                dto.setDesign(design.textValue());
            }
        }

        return dto;
    }

    private StandardFeatureAnalysisType getType(JsonNode jsonNode) {
        final var type = jsonNode.get("type");
        StandardFeatureAnalysisType result = null;
        if (Objects.nonNull(type) && !type.isNull()) {
            result = StandardFeatureAnalysisType.valueOf(type.textValue());
        }
        return result;
    }

    private FeAnalysisDTO createDto(JsonNode jsonNode) {
        final var type = getType(jsonNode);
        FeAnalysisDTO analysisDTO;
        if (Objects.equals(StandardFeatureAnalysisType.CRITERIA_SET, type)) {
            analysisDTO = new FeAnalysisWithConceptSetDTO();
        } else {
            analysisDTO = new FeAnalysisDTO();
        }
        return analysisDTO;
    }

    private BaseFeAnalysisCriteriaDTO convert(final JsonNode node) {
        try {
            return objectMapper.treeToValue(node, BaseFeAnalysisCriteriaDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
