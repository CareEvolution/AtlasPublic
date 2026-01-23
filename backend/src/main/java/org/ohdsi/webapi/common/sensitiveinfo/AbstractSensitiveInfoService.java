package org.ohdsi.webapi.common.sensitiveinfo;

import org.apache.commons.csv.CSVFormat;
import org.ohdsi.webapi.resourceutilities.StringResourceReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class AbstractSensitiveInfoService {
    protected static final Logger LOGGER = LoggerFactory.getLogger(SensitiveFilter.class);

    private final Collection<SensitiveFilter> filters;

    public AbstractSensitiveInfoService(StringResourceReader resourceReader) {
        var filterSource = resourceReader.getText("/generation/sensitive_filters.csv");
        filters = new ArrayList<>();
        try (var records = CSVFormat.RFC4180.parse(new StringReader(filterSource))) {
            for (var record : records) {
                filters.add(new SensitiveFilter(record.get(0), record.get(1)));
            }
        }
        catch (IOException e) {
            throw new BeanInitializationException("Failed to read sensitive_filters.csv", e);
        }
    }

    protected String filterSensitiveInfo(String text, Map<String, Object> variables, boolean isAdmin) {
        var result = text;
        if (Objects.nonNull(result) && !isAdmin) {
            var parser = new SpelExpressionParser();
            var context = new StandardEvaluationContext();
            if (Objects.nonNull(variables)) {
                context.setVariables(variables);
            }

            // Apply filter
            for (var filter : filters) {
                try {
                    var value = parser.parseExpression(filter.expression).getValue(context, String.class);
                    result = result.replaceAll(filter.regex, value);
                } catch (ParseException e) {
                    LOGGER.warn("Cannot parse expression: {}", filter.expression, e);
                }
            }
        }
        return result;
    }

    record SensitiveFilter(String regex, String expression) {}
}
