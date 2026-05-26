package cn.hsine.mem0.core.repository.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.UUID;

/**
 * MyBatis TypeHandler for PostgreSQL UUID <-> java.util.UUID conversion.
 *
 * @author MoBai

 */
public class UuidTypeHandler extends BaseTypeHandler<UUID> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, parameter);
    }

    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        return value instanceof UUID ? (UUID) value : (value != null ? UUID.fromString(value.toString()) : null);
    }

    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object value = rs.getObject(columnIndex);
        return value instanceof UUID ? (UUID) value : (value != null ? UUID.fromString(value.toString()) : null);
    }

    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object value = cs.getObject(columnIndex);
        return value instanceof UUID ? (UUID) value : (value != null ? UUID.fromString(value.toString()) : null);
    }
}
