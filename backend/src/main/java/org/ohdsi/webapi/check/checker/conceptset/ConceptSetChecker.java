package org.ohdsi.webapi.check.checker.conceptset;

import org.ohdsi.webapi.check.builder.ValidatorGroupBuilder;
import org.ohdsi.webapi.check.checker.BaseChecker;
import org.ohdsi.webapi.check.checker.tag.helper.TagHelper;
import org.ohdsi.webapi.conceptset.dto.ConceptSetDTO;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ConceptSetChecker extends BaseChecker<ConceptSetDTO> {
    private final TagHelper<ConceptSetDTO> tagHelper;

    public ConceptSetChecker(TagHelper<ConceptSetDTO> tagHelper) {
        this.tagHelper = tagHelper;
        createValidator();
    }

    @Override
    protected List<ValidatorGroupBuilder<ConceptSetDTO, ?>> getGroupBuilder() {
        return Arrays.asList(tagHelper.prepareTagBuilder());
    }
}
