package de.juniorjacki.SQL.Type;

import de.juniorjacki.SQL.Interface.InterDefinitions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Enum defining supported database types, their conversion logic, SQL type mappings, and optional alias types.
 */
public enum DatabaseType {
    // Add New Type:
    /*
    NAME(Type.class,
        List.of(Alias Classes),
        -> Append value (Type.class) to StringBuilder
        -> Get Value of your Type from a ResultSet
        -> Put Value of your Type into a PreparedStatement
        -> If your Type accepts extendedLength How the Table Builder should tell it the Database
     */

    STRING(String.class,
           List.of(CharSequence.class),
           (sb, value) -> InterDefinitions.appendEscapedString(sb, (String) value),
            ResultSet::getString,
           (ps, idx, val) -> ps.setString(idx, (String) val),
           (extendedLength) -> extendedLength == 0 ? "VARCHAR(255)" : "VARCHAR(" + extendedLength + ")"),

    UUID(UUID.class,
         List.of(),
         (sb, value) -> InterDefinitions.appendHexBytes(sb, TypeConverter.convertUUIDToBytes((UUID) value)),
         (rs, col) -> TypeConverter.convertBytesToUUID(rs.getBytes(col)),
         (ps, idx, val) -> ps.setBytes(idx, TypeConverter.convertUUIDToBytes((UUID) val)),
         (extendedLength) -> "BINARY(16)"),

    INTEGER(Integer.class,
            List.of(int.class),
            (sb, value) -> sb.append((Integer) value),
            ResultSet::getInt,
            (ps, idx, val) -> ps.setInt(idx, (Integer) val),
            (extendedLength) -> "INT"),

    LONG(Long.class,
         List.of(long.class),
         (sb, value) -> sb.append((Long) value),
            ResultSet::getLong,
         (ps, idx, val) -> ps.setLong(idx, (Long) val),
         (extendedLength) -> "BIGINT"),

    BYTE_ARRAY(byte[].class,
               List.of(),
               (sb, value) -> InterDefinitions.appendHexBytes(sb, (byte[]) value),
            ResultSet::getBytes,
               (ps, idx, val) -> ps.setBytes(idx, (byte[]) val),
               (extendedLength) -> extendedLength == 0 ? "BINARY(64)" : "BINARY(" + extendedLength + ")"),

    DOUBLE(Double.class,
           List.of(double.class),
           (sb, value) -> sb.append((Double) value),
            ResultSet::getDouble,
           (ps, idx, val) -> ps.setDouble(idx, (Double) val),
           (extendedLength) -> "DOUBLE"),

    BOOLEAN(Boolean.class,
            List.of(boolean.class),
            (sb, value) -> sb.append((Boolean) value ? "TRUE" : "FALSE"),
            (rs, col) -> rs.getBoolean(col),
            (ps, idx, val) -> ps.setBoolean(idx, (Boolean) val),
            (extendedLength) -> "BOOLEAN");

    private final Class<?> type;
    private final Set<Class<?>> aliasTypes;
    public final BiConsumer<StringBuilder, Object> appendConverter;
    public final TriFunction<ResultSet, String, Object> resultSetConverter;
    public final TriConsumer<PreparedStatement, Integer, Object> parameterSetter;
    private final Function<Integer, String> sqlTypeMapper;

    DatabaseType(Class<?> type,
                 BiConsumer<StringBuilder, Object> appendConverter,
                 TriFunction<ResultSet, String, Object> resultSetConverter,
                 TriConsumer<PreparedStatement, Integer, Object> parameterSetter,
                 Function<Integer, String> sqlTypeMapper) {
        this(type, List.of(), appendConverter, resultSetConverter, parameterSetter, sqlTypeMapper);
    }

    DatabaseType(Class<?> type,
                 List<Class<?>> aliasTypes,
                 BiConsumer<StringBuilder, Object> appendConverter,
                 TriFunction<ResultSet, String, Object> resultSetConverter,
                 TriConsumer<PreparedStatement, Integer, Object> parameterSetter,
                 Function<Integer, String> sqlTypeMapper) {
        this.type = type;
        this.aliasTypes = new HashSet<>(aliasTypes);
        this.appendConverter = appendConverter;
        this.resultSetConverter = resultSetConverter;
        this.parameterSetter = parameterSetter;
        this.sqlTypeMapper = sqlTypeMapper;
    }

    public Class<?> getTypeClass() {
        return type;
    }

    /**
     * Functional interface for setting PreparedStatement parameters.
     */
    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v) throws SQLException;
    }

    /**
     * Functional interface for Getting Results.
     */
    @FunctionalInterface
    public interface TriFunction<T, U, V> {
       V apply(T t, U u) throws SQLException;
    }

    /**
     * Finds the DatabaseType for a given class, mapping primary types, alias types, and primitive types to their
     * corresponding DatabaseType, defaulting to STRING for unknown types.
     *
     * @param type The class to find the DatabaseType for
     * @return The corresponding DatabaseType
     */
    public static DatabaseType forClass(Class<?> type) {
        for (DatabaseType dbType : values()) {
            if (dbType.type == type || dbType.aliasTypes.contains(type)) {
                return dbType;
            }
        }
        return STRING; // Fallback for unknown types
    }

    /**
     * Returns the SQL type for this DatabaseType, considering the extended length.
     *
     * @param extendedLength The length for types like VARCHAR or BINARY, or 0 for default
     * @return The SQL type as a string
     */
    public String getSQLType(int extendedLength) {
        return sqlTypeMapper.apply(extendedLength);
    }
}
