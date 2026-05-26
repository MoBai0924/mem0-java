package cn.hsine.mem0.core.vectorstore.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes(Object.class)
public class JsonbTypeHandler extends BaseTypeHandler<Object> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 【写入数据库】Java Object → JSONB
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        try {
            PGobject pgo = new PGobject();
            pgo.setType("jsonb");
            pgo.setValue(objectMapper.writeValueAsString(parameter));
            ps.setObject(i, pgo);
        } catch (JsonProcessingException e) {
            throw new SQLException("JSON serialization error", e);
        }
    }

    // 【从数据库读取】JSONB → Java Object
    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        PGobject pgo = rs.getObject(columnName, PGobject.class);
        if (pgo == null) return null;
        try {
            // ✅ 就是你这句！
            return objectMapper.readValue(pgo.getValue(), Object.class);
        } catch (JsonProcessingException e) {
            throw new SQLException("JSON deserialization error", e);
        }
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        PGobject pgo = rs.getObject(columnIndex, PGobject.class);
        if (pgo == null) return null;
        try {
            // ✅ 就是你这句！
            return objectMapper.readValue(pgo.getValue(), Object.class);
        } catch (JsonProcessingException e) {
            throw new SQLException("JSON deserialization error", e);
        }
    }

    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        PGobject pgo = cs.getObject(columnIndex, PGobject.class);
        if (pgo == null) return null;
        try {
            // ✅ 就是你这句！
            return objectMapper.readValue(pgo.getValue(), Object.class);
        } catch (JsonProcessingException e) {
            throw new SQLException("JSON deserialization error", e);
        }
    }
}
