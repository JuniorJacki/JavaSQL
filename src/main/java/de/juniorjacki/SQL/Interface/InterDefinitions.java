package de.juniorjacki.SQL.Interface;

import de.juniorjacki.SQL.Base.SQLInputFilter;
import de.juniorjacki.SQL.Structure.DatabaseProperty;
import de.juniorjacki.SQL.Structure.Table;
import de.juniorjacki.SQL.Type.DatabaseType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HexFormat;

public class InterDefinitions {
    /**
     * Appends a parameter value to a StringBuilder in a format suitable for SQL queries.
     * Applies SQL injection filtering and handles various data types appropriately.
     *
     * @param sb    The StringBuilder to append the parameter to
     * @param value The value to append
     * @throws RuntimeException if an error occurs during processing
     */
    public static void appendParameter(StringBuilder sb, Object value) {
        try {
            value = SQLInputFilter.filterExternalInput(value); // Filter SQL Injection
            if (value == null) {
                sb.append("NULL");
                return;
            }
            DatabaseType.forClass(value.getClass()).appendConverter.accept(sb, value);
        } catch (Exception e) {
            throwDBError(e);
        }
    }

    /**
     * Retrieves a typed value from a ResultSet for the specified column and table.
     *
     * @param rs           The ResultSet containing the query results
     * @param returnColumn The column to retrieve
     * @param table        The table associated with the column (optional, for qualifying column names)
     * @return The typed value of the column
     * @throws IllegalArgumentException if returnColumn is null
     * @throws SQLException if a database error occurs
     */
    public static  Object getTypedValue(ResultSet rs, DatabaseProperty returnColumn, Table<?, ?> table) throws SQLException {
        if (returnColumn == null) {
            throw new IllegalArgumentException("Return column cannot be null");
        }
        String columnName = returnColumn.name();
        if (table != null) {
            columnName = table.tableName() + "." + columnName;
        }
        return returnColumn.getType().resultSetConverter.apply(rs, columnName);
    }

    /**
     * Sets a parameter value in a PreparedStatement at the specified index.
     *
     * @param prepStatement The PreparedStatement to set the parameter on
     * @param index        The parameter index
     * @param value        The value to set
     * @throws SQLException if a database error occurs
     */
    public static void setParameter(PreparedStatement prepStatement, int index, Object value) throws SQLException {
        try {
            value = SQLInputFilter.filterExternalInput(value); // Filter SQL Injection
            if (value == null) {
                prepStatement.setNull(index, Types.VARCHAR);
                return;
            }
            DatabaseType.forClass(value.getClass()).parameterSetter.accept(prepStatement, index, value);
        } catch (Exception e) {
            throwDBError(e);
        }
    }

    /**
     * Sets a constructor argument from a ResultSet for a specific property and component name.
     *
     * @param resultSet      The ResultSet containing the query results
     * @param property       The property to retrieve
     * @param constructorArgs The array to store the constructor argument
     * @param index          The index in the constructorArgs array
     * @param componentName  The column name in the ResultSet
     * @param <E>           The enum type representing properties of the table
     * @throws SQLException if a database error occurs
     */
    public static <E extends Enum<E> & DatabaseProperty> void setConstructorArg(ResultSet resultSet, E property, Object[] constructorArgs, int index, String componentName) throws SQLException {
        if (property == null) {
            throw new IllegalArgumentException("Property cannot be null");
        }
        constructorArgs[index] = property.getType().resultSetConverter.apply(resultSet, componentName);
    }

    /**
     * Returns the SQL type for a given Java class, considering an extended length for certain types.
     *
     * @param dataType      The Java class to map to an SQL type
     * @param extendedLength The length for types like VARCHAR or BINARY, or 0 for default
     * @return The SQL type as a string
     * @throws IllegalArgumentException if dataType is null
     */
    public static String getSQLType(DatabaseType dataType, int extendedLength) {
        if (dataType == null) {
            throw new IllegalArgumentException("Data type cannot be null");
        }
        return dataType.getSQLType(extendedLength);
    }

    /**
     * Appends an escaped string to a StringBuilder, suitable for SQL queries.
     *
     * @param sb    The StringBuilder to append to
     * @param value The string value to escape and append
     */
    public static void appendEscapedString(StringBuilder sb, String value) {
        if (value == null) {
            sb.append("NULL");
        } else {
            sb.append("'").append(value.replace("'", "''")).append("'");
        }
    }

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    /**
     * Appends a byte array as a hexadecimal string to a StringBuilder, suitable for SQL queries.
     *
     * @param sb    The StringBuilder to append to
     * @param bytes The byte array to convert and append
     */
    public static void appendHexBytes(StringBuilder sb, byte[] bytes) {
        if (bytes == null) {
            sb.append("NULL");
        } else {
            sb.append("X'").append(HEX_FORMAT.formatHex(bytes)).append("'");
        }
    }

    /*
    public static void appendParameter(StringBuilder sb, Object value) {
        try {
            value = SQLInputFilter.filterExternalInput(value); // Filter SQL Injection

            if (value == null) {
                sb.append("NULL");
                return;
            }
            switch (value) {
                case String s -> appendEscapedString(sb, s);
                case UUID uuid -> appendHexBytes(sb, TypeConverter.convertUUIDToBytes(uuid));
                case Integer i -> sb.append(i);
                case Long l -> sb.append(l);
                case byte[] bytes -> appendHexBytes(sb, bytes);
                case BlobString blob -> appendHexBytes(sb,blob.getBytes());
                case NodeType nd -> sb.append(nd.ordinal());
                case JSONObject json -> appendEscapedString(sb, json.toString());
                case Double d -> sb.append(d);
                case Boolean b -> sb.append(b ? "TRUE" : "FALSE");
                default -> appendEscapedString(sb, value.toString());
            }
        } catch (Exception e) {
            throwDBError(e);
            return;
        }
    }

    public static Object getTypedValue(ResultSet rs, DatabaseProperty returnColumn, Table<?,?> table) throws SQLException {
        String columnName = returnColumn.name();
        if (table != null) {
            columnName = table.tableName() + "." + columnName;
        }
        Class<?> type = returnColumn.getType();
        if (type == String.class) {
            return rs.getString(columnName);
        } else if (type == UUID.class) {
            return TypeConverter.convertBytesToUUID(rs.getBytes(columnName));
        } else if (type == byte[].class) {
            return rs.getBytes(columnName);
        } else if (type == Integer.class) {
            return rs.getInt(columnName);
        } else if (type == BlobString.class) {
            return new BlobString(rs.getBytes(columnName));
        } else if (type == Long.class || type == long.class) {
            return rs.getLong(columnName);
        } else if (type == JSONObject.class) {
            return new JSONObject(rs.getString(columnName));
        } else if (type == Double.class) {
            return rs.getDouble(columnName);
        } else {
            return rs.getObject(columnName);
        }
    }

    public static void setParameter(PreparedStatement prepStatement, int index, Object value) throws SQLException {
        switch (value) {
            case String s -> prepStatement.setString(index, s);
            case UUID uuid -> prepStatement.setBytes(index, TypeConverter.convertUUIDToBytes(uuid));
            case Integer i -> prepStatement.setInt(index, i);
            case byte[] b -> prepStatement.setBytes(index, b);
            case long ll -> prepStatement.setLong(index, ll);
            case BlobString bs -> prepStatement.setBytes(index, bs.getBytes());
            case NodeType nd -> prepStatement.setInt(index, nd.ordinal());
            case JSONObject jObject -> prepStatement.setString(index, jObject.toString());
            case Double d -> prepStatement.setDouble(index, d);
            case Boolean b -> prepStatement.setBoolean(index, b);
            case null -> prepStatement.setNull(index, Types.VARCHAR);
            default -> prepStatement.setObject(index, value);
        }
    }

    public static <E extends Enum<E> & DatabaseProperty> void setConstructorArg(ResultSet resultSet, E property, Object[] constructorArgs, int i, String componentName) throws SQLException {
        if (property.getType().equals(String.class)) {
            constructorArgs[i] = resultSet.getString(componentName);
        } else if (property.getType().equals(UUID.class)) {
            constructorArgs[i] = TypeConverter.convertBytesToUUID(resultSet.getBytes(componentName));
        } else if (property.getType().equals(Integer.class)) {
            constructorArgs[i] = resultSet.getInt(componentName);
        } else if (property.getType().equals(NodeType.class)) {
            constructorArgs[i] = NodeType.values()[resultSet.getInt(componentName)];
        } else if (property.getType().equals(JSONObject.class)) {
            constructorArgs[i] = new JSONObject(resultSet.getString(componentName));
        } else if (property.getType().equals(byte[].class)) {
            constructorArgs[i] = resultSet.getBytes(componentName);
        } else if (property.getType().equals(BlobString.class)) {
            constructorArgs[i] = new BlobString(resultSet.getBytes(componentName));
        } else if (property.getType().equals(Long.class)) {
            constructorArgs[i] = resultSet.getLong(componentName);
        } else if (property.getType().equals(Double.class)) {
            constructorArgs[i] = resultSet.getDouble(componentName);
        } else if (property.getType().equals(Boolean.class)) {
            constructorArgs[i] = resultSet.getBoolean(componentName);
        } else {
            constructorArgs[i] = resultSet.getObject(componentName);
        }
    }

    public static String getSQLType(Class<?> dataType, int extendedLength) {
        return switch (dataType.getSimpleName()) {
            case "UUID" -> "BINARY(16)";
            case "String" -> {
                if (extendedLength == 0) yield "VARCHAR(255)";
                yield "VARCHAR("+extendedLength+")";
            }
            case "NodeType" -> "TINYINT";
            case "byte[]" -> {
                if (extendedLength == 0) yield "BINARY(64)";
                yield "BINARY("+extendedLength+")";
            }
            case "BlobString" -> "BLOB";
            case "JSONObject" -> "JSON";
            case "Long", "long" -> "BIGINT";
            case "Integer" -> "INT";
            case "Boolean" -> "BOOLEAN";
            case "Double" -> "DOUBLE";
            default -> "UNKNOWN";
        };
    }

    private static void appendEscapedString(StringBuilder sb, String value) {
        sb.append("'").append(value.replace("'", "''")).append("'");
    }

    private static void appendHexBytes(StringBuilder sb, byte[] bytes) {
        sb.append("X'").append(bytesToHex(bytes)).append("'");
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        return HexFormat.of().formatHex(bytes); // Java 17+ for efficient hex conversion
    }


     */
    public enum CompareOperator {
        EQUALS("="),
        GREATER_THAN(">"),
        LESS_THAN("<"),
        GREATER_THAN_OR_EQUAL(">="),
        LESS_THAN_OR_EQUAL("<="),
        NOT_EQUAL("<>"),
        ;
        public final String sql;
        CompareOperator(String sql) {
            this.sql = sql;
        }
    }

    private static void throwDBError(Exception e){
       e.printStackTrace();
    }
}
