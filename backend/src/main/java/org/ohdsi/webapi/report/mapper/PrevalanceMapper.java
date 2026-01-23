package org.ohdsi.webapi.report.mapper;

import org.ohdsi.webapi.report.PrevalenceRecord;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PrevalanceMapper implements RowMapper<PrevalenceRecord> {
    @Override
    public PrevalenceRecord mapRow(ResultSet rs, int rowNum)
            throws SQLException {
        var record = new PrevalenceRecord();
        record.setyPrevalence1000Pp(rs.getDouble("Y_PREVALENCE_1000PP"));
        record.setxCalendarMonth(rs.getInt("X_CALENDAR_MONTH"));
        // TODO why is the below statement commented out?
        //		record.setNumPersons(rs.getInt("NUM_PERSONS"));
        return record;
    }
}
