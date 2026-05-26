package com.mem0.core.repository.handler;

import com.mem0.core.domain.model.MemoryEvent;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler for MemoryEvent enum <-> String conversion.
 *
 * @author MoBai

 */
public class MemoryEventTypeHandler extends BaseTypeHandler<MemoryEvent> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, MemoryEvent parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public MemoryEvent getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value != null ? MemoryEvent.valueOf(value) : null;
    }

    @Override
    public MemoryEvent getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value != null ? MemoryEvent.valueOf(value) : null;
    }

    @Override
    public MemoryEvent getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value != null ? MemoryEvent.valueOf(value) : null;
    }
}
