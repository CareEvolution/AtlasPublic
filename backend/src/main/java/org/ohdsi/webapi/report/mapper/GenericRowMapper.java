package org.ohdsi.webapi.report.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.WordUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class GenericRowMapper implements RowMapper<JsonNode> {
    private final ObjectMapper mapper;

    public GenericRowMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public JsonNode mapRow(ResultSet rs, int rowNum) throws SQLException {
        var objectNode = mapper.createObjectNode();
        var rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        for (int index = 1; index <= columnCount; index++) {
            var column = JdbcUtils.lookupColumnName(rsmd, index);
            var value = rs.getObject(column);
            column = this.snakeCaseToCamelCase(column);

            switch (value) {
                case null -> objectNode.putNull(column);
                case Integer i -> objectNode.put(column, i);
                case String s -> objectNode.put(column, s);
                case Boolean b -> objectNode.put(column, b);
                case Date date -> objectNode.put(column, date.getTime());
                case Long l -> objectNode.put(column, l);
                case Double v -> objectNode.put(column, v);
                case Float v -> objectNode.put(column, v);
                case BigDecimal bigDecimal -> objectNode.put(column, bigDecimal);
                case Byte b -> objectNode.put(column, b);
                case byte[] bytes -> objectNode.put(column, bytes);
                default -> throw new IllegalArgumentException("Unmappable object type: " + value.getClass());
            }
        }
        return objectNode;
    }

    protected String snakeCaseToCamelCase(String str) {
        char[] delimeters = new char[] { '_' };
        str = WordUtils.capitalizeFully(str.toLowerCase(), delimeters)
                .replace("_", "");
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}
