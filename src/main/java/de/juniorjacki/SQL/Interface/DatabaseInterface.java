package de.juniorjacki.SQL.Interface;

import de.juniorjacki.SQL.Query.SQLInputFilter;
import de.juniorjacki.SQL.SQL;
import de.juniorjacki.SQL.Structure.DatabaseProperty;
import de.juniorjacki.SQL.Structure.DatabaseRecord;
import de.juniorjacki.SQL.Structure.Table;

import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static de.juniorjacki.utils.Record.getValueByName;

public interface DatabaseInterface<G extends Table<E, R>, R extends java.lang.Record & DatabaseRecord<R, E>, E extends Enum<E> & DatabaseProperty> {


    G getInstance();

    enum Order{
        ASCENDING("ASC"),
        DESCENDING("DESC");

        private final String sql;
        Order(String sql) {
            this.sql = sql;
        }
    }

    /**
     * Record representing a key-value pair for database query
     */
    record ColumnValue<E>(E keyColumn, Object keyValue) {}


    private void throwDBError(Exception e){
        System.out.println("Database Error: " + e);
    }
    
    private void throwInputError(E keyColumn, Object keyValue) {
        System.out.println("Invalid value type for {}: {}, expected: {}",
                keyColumn.name(), keyValue.getClass().getName(), keyColumn.getType().getName());
    }

    /**
     * Gets the count of specified data from the database.
     * @return The count of rows found
     */
    default int countByValue(E keyColumn, Object keyValue) {
        try {
            keyValue = SQLInputFilter.filterExternalInput(keyValue); // Filter SQL Injection
            if (!keyColumn.getType().isInstance(keyValue)) {
                throwInputError(keyColumn, keyValue);
                return 0;
            }
            try (PreparedStatement prepStatement = SQL.getConnection().get().prepareStatement("SELECT COUNT(*) FROM " + getInstance().tableName() + " WHERE " + keyColumn.name() + " = ?")) {
                setParameter(prepStatement, 1, keyValue);
                try (ResultSet rs = prepStatement.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    } else {
                        return 0;
                    }
                }
            }
        }
        catch (Exception e) {
            throwDBError(e);
            return 0;
        }
    }

    /**
     * Checks if the specified data exists in the database.
     * @return true if the element exists, false otherwise
     */
    default boolean existsByKey(E keyColumn, Object keyValue) {
        try {
            keyValue = SQLInputFilter.filterExternalInput(keyValue); // Filter SQL Injection
            if (!keyColumn.getType().isInstance(keyValue)) {
                throwInputError(keyColumn, keyValue);
                return false;
            }

            try (PreparedStatement prepStatement = SQL.getConnection().get().prepareStatement("SELECT 1 FROM " + getInstance().tableName() + " WHERE " + keyColumn.name() + " = ? LIMIT 1")) {
                setParameter(prepStatement, 1, keyValue);
                try (ResultSet rs = prepStatement.executeQuery()) {
                    return rs.next();
                }
            }
        }
        catch (Exception e) {
            throwDBError(e);
            return false;
        }
    }

    /**
     * Checks if the specified data exists in the database based on key-value pairs.
     * @param pairs One or more column-value pairs to check
     * @return true if the element exists, false otherwise
     */
    default boolean existsByKeys(ColumnValue<E>... pairs) {
        try {
            if (pairs == null || pairs.length == 0) {
                LogManager.getLogger().error("At least one column-value pair must be provided");
            }
            StringBuilder whereClause = new StringBuilder();
            int paramCount = 0;
            for (ColumnValue<E> pair : pairs) {
                E keyColumn = pair.keyColumn();
                Object keyValue = SQLInputFilter.filterExternalInput(pair.keyValue());

                if (!keyColumn.getType().isInstance(keyValue)) {
                    throwInputError(keyColumn, keyValue);
                    return false;
                }

                if (paramCount > 0) {
                    whereClause.append(" AND ");
                }
                whereClause.append(keyColumn.name()).append(" = ?");
                paramCount++;
            }
            try (PreparedStatement prepStatement = SQL.getConnection().get()
                    .prepareStatement("SELECT 1 FROM " + getInstance().tableName() + " WHERE " + whereClause + " LIMIT 1")) {
                for (int i = 0; i < pairs.length; i++) {
                    setParameter(prepStatement, i + 1, pairs[i].keyValue());
                }
                try (ResultSet rs = prepStatement.executeQuery()) {
                    return rs.next();
                }
            }
        }
        catch (Exception e) {
            throwDBError(e);
            return false;
        }
    }

    /**
     * Checks if the specified data exists in the database based on any key-value pairs.
     * @param pairs One or more column-value pairs to check
     * @return true if any of the pairs exists, false otherwise
     */
    default boolean existsByAnyKeys(ColumnValue<E>... pairs) {
        try {
            if (pairs == null || pairs.length == 0) {
                LogManager.getLogger().error("At least one column-value pair must be provided");
            }
            StringBuilder whereClause = new StringBuilder();
            int paramCount = 0;
            for (ColumnValue<E> pair : pairs) {
                E keyColumn = pair.keyColumn();
                Object keyValue = SQLInputFilter.filterExternalInput(pair.keyValue());

                if (!keyColumn.getType().isInstance(keyValue)) {
                    throwInputError(keyColumn, keyValue);
                    return false;
                }

                if (paramCount > 0) {
                    whereClause.append(" OR ");
                }
                whereClause.append(keyColumn.name()).append(" = ?");
                paramCount++;
            }

            try (PreparedStatement prepStatement = SQL.getConnection().get()
                    .prepareStatement("SELECT 1 FROM " + getInstance().tableName() + " WHERE " + whereClause + " LIMIT 1")) {
                for (int i = 0; i < pairs.length; i++) {
                    setParameter(prepStatement, i + 1, pairs[i].keyValue());
                }
                try (ResultSet rs = prepStatement.executeQuery()) {
                    return rs.next();
                }
            }
        }
        catch (Exception e) {
            throwDBError(e);
            return false;
        }
    }

    /**
     * Checks if the specified data exists in the database based on any key-value pairs.
     * @param pairs One or more column-value pairs to check
     * @return true if any of the pairs exists, false otherwise
     */
    default boolean existsByKeyAndAnyValues(ColumnValue<E> keyPair,ColumnValue<E>... pairs) {
        try {
            if (keyPair == null || pairs == null || pairs.length == 0) {
                LogManager.getLogger().error("At least one column-value pair must be provided");
            }
            StringBuilder whereClause = new StringBuilder();
            int paramCount = 0;

            E keyColumn = keyPair.keyColumn();
            Object keyValue = SQLInputFilter.filterExternalInput(keyPair.keyValue());
            whereClause.append(keyColumn.name()).append(" = ?");
            whereClause.append(" AND (");

            for (ColumnValue<E> pair : pairs) {
                E column = pair.keyColumn();
                Object value = SQLInputFilter.filterExternalInput(pair.keyValue());

                if (!column.getType().isInstance(value)) {
                    throwInputError(column, value);
                    return false;
                }

                if (paramCount > 0) {
                    whereClause.append(" OR ");
                }
                whereClause.append(column.name()).append(" = ?");
                paramCount++;
            }
            whereClause.append(" )");

            try (PreparedStatement prepStatement = SQL.getConnection().get()
                    .prepareStatement("SELECT 1 FROM " + getInstance().tableName() + " WHERE " + whereClause + " LIMIT 1")) {
                setParameter(prepStatement, 1, keyValue);
                for (int i = 0; i < pairs.length; i++) {
                    setParameter(prepStatement, i + 2, pairs[i].keyValue());
                }
                try (ResultSet rs = prepStatement.executeQuery()) {
                    return rs.next();
                }
            }
        }
        catch (Exception e) {
            throwDBError(e);
            return false;
        }
    }

    /**
     * Gets Specified Data from Database
     * @return List of Table Record
     */
    default Optional<List<R>> getByKey(E keyColumn, Object keyValue) {
        try {
            keyValue = SQLInputFilter.filterExternalInput(keyValue); // Filter SQL Injection
            if (!keyColumn.getType().isInstance(keyValue)) {
                throwInputError(keyColumn, keyValue);
                return Optional.empty();
            }
            try (PreparedStatement prepStatement = SQL.getConnection().get().prepareStatement("SELECT * FROM " + getInstance().tableName() + " WHERE " + keyColumn.name() + " = ?")) {
                setParameter(prepStatement, 1, keyValue);
                try (ResultSet rs = prepStatement.executeQuery()) {
                    List<R> objects = new ArrayList<>();
                    while (rs.next()) {
                        objects.add((R) Record.populateRecord(getInstance(), rs));
                    }
                    return Optional.of(objects);
                }
            }
        }
        catch (Exception e) {
            throwDBError(e);
            return Optional.empty();
        }
    }

    /**
     * Gets Specified Data from Database
     * @return First Table Record
     */
    default Optional<R> getFirstByKey(E keyColumn, Object keyValue) {
        try {
            keyValue = SQLInputFilter.filterExternalInput(keyValue); // Filter SQL Injection
            if (!keyColumn.getType().isInstance(keyValue)) {
                throwInputError(keyColumn, keyValue);
                return Optional.empty();
            }
            try (PreparedStatement prepStatement = SQL.getConnection().get().prepareStatement("SELECT * FROM " + getInstance().tableName() + " WHERE " + keyColumn.name() + " = ? LIMIT 1")) {
                setParameter(prepStatement, 1, keyValue);
                try (ResultSet rs = prepStatement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of((R) Record.populateRecord(getInstance(), rs));
                    }
                    return Optional.empty();
                }
            }
        }
        catch (Exception e) {
            throwDBError(e);
            return Optional.empty();
        }
    }

    /**
     * Gets Specified Data from Database
     * @return Returns List of Column Type Objects as List
     */
    default Optional<Object> getFirstColumnByKey(E keyColumn, Object keyValue, E returnColumn) {
        try {
            keyValue = SQLInputFilter.filterExternalInput(keyValue); // Filter SQL Injection
            if (!keyColumn.getType().isInstance(keyValue)) {
                throwInputError(keyColumn, keyValue);
                return Optional.empty();
            }
            try (PreparedStatement prepStatement = SQL.getConnection().get().prepareStatement(String.format("SELECT %s FROM %s WHERE %s = ? LIMIT 1", returnColumn.name(), getInstance().tableName(), keyColumn.name()))) {
                setParameter(prepStatement, 1, keyValue);
                try (ResultSet rs = prepStatement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(getTypedValue(rs, returnColumn));
                    }
                    return Optional.empty();
                }
            }
        }
        catch (Exception e) {
            throwDBError(e);
            return Optional.empty();
        }
    }

    /**
     * Gets the first or last value from in the database, based on the orderColumn.
     * @param orderColumn The column to filter by
     * @param order The order (ascending or descending)
     * @return Returns a single table record as an Object
     */
    default Optional<R> getByOrder(E orderColumn, Order order) {
        try {
            String query = String.format("SELECT * FROM %s ORDER BY %s %s LIMIT 1", getInstance().tableName(), orderColumn.name(), order.sql);
            try (PreparedStatement prepStatement = SQL.getConnection().get().prepareStatement(query)) {
                try (ResultSet rs = prepStatement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of((R) Record.populateRecord(getInstance(), rs));
                    } else {
                        return Optional.empty();
                    }
                }
            }
        }
        catch (Exception e) {
            throwDBError(e);
            return Optional.empty();
        }
    }

    /**
     * Gets the first or last value from in the database, based on the Key.
     * @param keyColumn The column to filter by
     * @param keyValue The value to filter by
     * @param orderColumn Column to order
     * @param order The order (ascending or descending)
     * @return Returns a single table record as an Object
     */
    default Optional<R> getByOrderAndKey(E keyColumn,Object keyValue,E orderColumn, Order order) {
        try {
            keyValue = SQLInputFilter.filterExternalInput(keyValue); // Filter SQL Injection
            if (!keyColumn.getType().isInstance(keyValue)) {
                throwInputError(keyColumn, keyValue);
                return Optional.empty();
            }
            String query = String.format("SELECT * FROM %s WHERE %s = ? ORDER BY %s %s LIMIT 1", getInstance().tableName(),keyColumn.name(), orderColumn.name(), order.sql);
            try (PreparedStatement prepStatement = SQL.getConnection().get().prepareStatement(query)) {
                setParameter(prepStatement, 1, keyValue);
                try (ResultSet rs = prepStatement.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of((R) Record.populateRecord(getInstance(), rs));
                    } else {
                        return Optional.empty();
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throwDBError(e);
            return Optional.empty();
        }
    }

    /**
     * Gets the first or last value from in the database, based on the Keys.
     * @param orderColumn Column to order
     * @param order The order (ascending or descending)
     * @return Returns a single table record as an Object
     */
    default Optional<R> getByOrderAndKeys(E orderColumn, Order order,ColumnValue<E>... keyPairs) {
        try {
            if (keyPairs == null || keyPairs.length == 0) {
                LogManager.getLogger().error("At least one column-value pair must be provided");
            }
            // Validate all parameters upfront
            for (ColumnValue<E> pair : keyPairs) {
                if (!pair.keyColumn().getType().isInstance(pair.keyValue())) {
                    throwInputError(pair.keyColumn(), pair.keyValue());
                    return Optional.empty();
                }
            }
            // Use Stream to build WHERE clause efficiently
            String whereClause = Arrays.stream(keyPairs)
                    .map(pair -> pair.keyColumn().name() + " = ?")
                    .collect(Collectors.joining(" AND "));

            String query = "SELECT * FROM " + getInstance().tableName() +
                    " WHERE " + whereClause +
                    " ORDER BY " + orderColumn.name() + " " + order.sql +
                    " LIMIT 1";
            LogManager.getLogger().info(query);
            return executeQuery(query, (ColumnValue<E>[]) keyPairs);
        }
        catch (Exception e) {
            e.printStackTrace();
            throwDBError(e);
            return Optional.empty();
        }
    }

    /**
     * Gets value from in the database, based on the Keys specified.
     * @return Returns a single table record as an Object
     */
    default Optional<R> getByKeys(ColumnValue<E>... keyPairs) {
        try {
            if (keyPairs == null || keyPairs.length == 0) {
                LogManager.getLogger().error("At least one column-value pair must be provided");
            }
            // Validate all parameters upfront
            for (ColumnValue<E> pair : keyPairs) {
                if (!pair.keyColumn().getType().isInstance(pair.keyValue())) {
                    throwInputError(pair.keyColumn(), pair.keyValue());
                    return Optional.empty();
                }
            }
            // Use Stream to build WHERE clause efficiently
            String whereClause = Arrays.stream(keyPairs)
                    .map(pair -> pair.keyColumn().name() + " = ?")
                    .collect(Collectors.joining(" AND "));

            String query = "SELECT * FROM " + getInstance().tableName() +
                    " WHERE " + whereClause +
                    " LIMIT 1";
            return executeQuery(query, (ColumnValue<E>[]) keyPairs);
        }
        catch (Exception e) {
            e.printStackTrace();
            throwDBError(e);
            return Optional.empty();
        }
    }



    /**
     * Gets Specified Data from Database
     * @param keyColumn
     * @param keyValue
     * @param returnColumn
     * @return Returns List of Column Type Objects as List
     */
    default Optional<List<Object>> getColumnByValue(E keyColumn, Object keyValue, E returnColumn) {
        try {
            keyValue = SQLInputFilter.filterExternalInput(keyValue); // Filter SQL Injection
            if (!keyColumn.getType().isInstance(keyValue)) {
                throwInputError(keyColumn, keyValue);
                return Optional.empty();
            }
            try (PreparedStatement prepStatement = SQL.getConnection().get().prepareStatement(String.format("SELECT %s FROM %s WHERE %s = ?", returnColumn.name(), getInstance().tableName(), keyColumn.name()))) {
                setParameter(prepStatement, 1, keyValue);
                try (ResultSet rs = prepStatement.executeQuery()) {
                    List<Object> resultList = new ArrayList<>();
                    while (rs.next()) {
                        resultList.add(getTypedValue(rs, returnColumn));
                    }
                    return Optional.of(resultList);
                }
            }
        }
        catch (Exception e) {
            throwDBError(e);
            return Optional.empty();
        }
    }


    /**
     * Gets the first or last value from the specified column in the database, based on the keyColumn.
     * @param keyColumn The column to order by
     * @param returnColumn The column to return
     * @param order The order (ascending or descending)
     * @return Returns a single column value as an Object
     */
    default Optional<Object> getColumnByOrder(E keyColumn, E returnColumn, Order order) {
        try {
            try (PreparedStatement prepStatement = SQL.getConnection().get().prepareStatement(String.format("SELECT %s FROM %s ORDER BY %s %s LIMIT 1", returnColumn.name(), getInstance().tableName(), keyColumn.name(), order.sql));
                 ResultSet rs = prepStatement.executeQuery()) {

                if (rs.next()) {
                    return Optional.of(getTypedValue(rs, returnColumn));
                } else {
                    return Optional.empty();
                }
            }
        }
        catch (Exception e) {
                throwDBError(e);
            return Optional.empty();
        }

    }

    /**
     * Updates specified Rows with specified Data
     * @return If Success returns True
     */
    default boolean update(E keyColumn, Object keyValue, E updateColumn, Object updateValue) {
        try {
            try (PreparedStatement preparedStatement = SQL.getConnection().get().prepareStatement("UPDATE "+getInstance().tableName() +" SET " + updateColumn.name() + " = ? WHERE " + keyColumn.name() + " = ?")) {
                setParameter(preparedStatement, 1, SQLInputFilter.filterExternalInput(updateValue));
                setParameter(preparedStatement, 2, SQLInputFilter.filterExternalInput(keyValue));
                int affectedRows = preparedStatement.executeUpdate();
                return affectedRows > 0;
            }
        }  catch (Exception e) {
            throwDBError(e);
            return false;
        }
    }


    /**
     * Updates the first row based on the specified key column and order, updating the updateColumn with updateValue.
     * @param keyColumn The column to order by
     * @param order The order (ascending or descending)
     * @param updateColumn The column to update
     * @param updateValue The value to update the column with
     * @return If success, returns true
     */
    default boolean updateByOrder(E keyColumn, Order order, E updateColumn, Object updateValue) {
        try {
            try (PreparedStatement preparedStatement = SQL.getConnection().get().prepareStatement(String.format("UPDATE %s SET %s = ? WHERE %s = (SELECT %s FROM %s ORDER BY %s %s LIMIT 1)",
                    getInstance().tableName(), updateColumn.name(), keyColumn.name(), keyColumn.name(), getInstance().tableName(), keyColumn.name(), order.sql))) {
                setParameter(preparedStatement, 1, SQLInputFilter.filterExternalInput(updateValue));
                int affectedRows = preparedStatement.executeUpdate();
                return affectedRows > 0;
            }
        }  catch (Exception e) {
            throwDBError(e);
            return false;
        }
    }

    /**
     * Updates Rows by specified Key Values in Record with Data of Record
     * @param record
     * @return If Success returns True
     * @throws Exception
     */
    default boolean update(R record) {
        try {
            record = SQLInputFilter.filterExternalInput(record); // Filter SQL Injection
            List<E> properties = getInstance().getProperties();
            String setClause = properties.stream()
                    .filter(property -> !property.isKey())
                    .map(property -> property.name() + " = ?")
                    .collect(Collectors.joining(", "));
            if (setClause.isEmpty()) {
                throw new IllegalArgumentException("No non-key properties to update");
            }
            try (PreparedStatement prepStatement = SQL.getConnection().get().prepareStatement(String.format("UPDATE %s SET %s WHERE %s;", getInstance().tableName(), setClause,
                    properties.stream().filter(E::isKey).map(property -> property.name() + " = ?").collect(Collectors.joining(" AND "))))) {
                int index = setParameters(prepStatement, record, properties, false);
                setParameters(prepStatement, record, properties, true, index);
                return prepStatement.executeUpdate() > 0;
            }
        }  catch (Exception e) {
            throwDBError(e);
            return false;
        }
    }

    /**
     * Upsert's entire Record into Database
     * @param record
     * @return If Success returns True
     * @throws Exception
     */
    default boolean upsert(R record) {
        try {
            record = SQLInputFilter.filterExternalInput(record); // Filter SQL Injection
            List<E> properties = getInstance().getProperties();
            try (PreparedStatement prepStatement = SQL.getConnection().get().prepareStatement(String.format("INSERT INTO %s (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s;",
                    getInstance().tableName(),
                    properties.stream().map(Enum::name).collect(Collectors.joining(", ")),
                    properties.stream().map(p -> "?").collect(Collectors.joining(", ")),
                    properties.stream().map(p -> p.name() + " = VALUES(" + p.name() + ")").collect(Collectors.joining(", "))
            ))) {
                setParameters(prepStatement, record, properties);
                return prepStatement.executeUpdate() > 0;
            }
        }  catch (Exception e) {
            throwDBError(e);
            return false;
        }

    }

    @NotNull
    private Optional<R> executeQuery(String query, ColumnValue<E>[] keyPairs) throws Exception {
        try (PreparedStatement prepStatement = SQL.getConnection().get().prepareStatement(query)) {
            for (int i = 0; i < keyPairs.length; i++) {
                setParameter(prepStatement, i + 1, SQLInputFilter.filterExternalInput(keyPairs[i].keyValue()));
            }
            try (ResultSet rs = prepStatement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of((R) Record.populateRecord(getInstance(), rs));
                } else {
                    return Optional.empty();
                }
            }
        }
    }


    private void setParameters(PreparedStatement prepStatement, R record, List<E> properties) throws SQLException, NoSuchFieldException, IllegalAccessException {
        for (int index = 0; index < properties.size(); index++) {
            setParameter(prepStatement, index + 1, getValueByName(record, properties.get(index).name()));
        }
    }

    private int setParameters(PreparedStatement prepStatement, R record, List<E> properties, boolean keysOnly) throws
            SQLException, NoSuchFieldException, IllegalAccessException {
        return setParameters(prepStatement, record, properties, keysOnly, 1);
    }

    private int setParameters(PreparedStatement prepStatement, R record, List<E> properties, boolean keysOnly,
                              int startIndex) throws SQLException, NoSuchFieldException, IllegalAccessException {
        int index = startIndex;
        for (E property : properties) {
            if (property.isKey() == keysOnly) {
                Object value = getValueByName(record, property.name());
                setParameter(prepStatement, index++, value);
            }
        }
        return index;
    }

    private void setParameter(PreparedStatement prepStatement, int index, Object value) throws SQLException {
        switch (value) {
            case String s -> prepStatement.setString(index, s);
            case UUID uuid -> prepStatement.setString(index, uuid.toString());
            case Integer i -> prepStatement.setInt(index, i);
            case long ll -> prepStatement.setLong(index, ll);
            case JSONObject jObject -> prepStatement.setString(index, jObject.toString());
            case Double d -> prepStatement.setDouble(index, d);
            case Boolean b -> prepStatement.setBoolean(index, b);
            default -> prepStatement.setObject(index, value);
        }
    }


    private Object getTypedValue(ResultSet rs, E returnColumn) throws SQLException {
        String columnName = returnColumn.name();
        Class<?> type = returnColumn.getType();
        if (type == String.class) {
            return rs.getString(columnName);
        } else if (type == UUID.class) {
            return UUID.fromString(rs.getString(columnName));
        } else if (type == Integer.class) {
            return rs.getInt(columnName);
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
}
