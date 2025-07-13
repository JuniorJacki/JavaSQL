# JavaSQL - A Lightweight, Highly Customizable SQL Service

## Key Features

- **Connection Management**
  - Automatically reconnects if connection is lost
  - Handles data usage during connection interruptions

- **Automatic Table Creation**
  - Creates tables based on definition in a Java Class
    
- **Easy Data Handling**
  - Automatic datatype conversion between database and Java
  - Rows as records
  - Columns as their Java datatypes

- **SQL Injection Protection**
  - Filters all input passed to built-in database methods

## Default Database Methods
### ([src/main/SQL/Interface/DatabaseInterface.java](https://github.com/JuniorJacki/JavaSQL/blob/main/src/main/java/de/juniorjacki/SQL/Interface/DatabaseInterface.java))

### Query Methods
- `int countByValue(Column keyColumn, Object keyValue)`
  - Returns the number of rows in the database table where the specified column matches the provided value.
- `boolean existsByKey(Column keyColumn, Object keyValue)`
  - Checks if at least one row exists in the database table where the specified column matches the provided value.
- `boolean existsByKeys(ColumnValue<Column>... pairs)`
  - Checks if at least one row exists in the database table where all specified column-value pairs match.
- `boolean existsByAnyKeys(ColumnValue<Column>... pairs)`
  - Checks if at least one row exists in the database table where any of the specified column-value pairs match.
- `boolean existsByKeyAndAnyValues(ColumnValue<Column> keyPair, ColumnValue<Column>... pairs)`
  - Checks if at least one row exists in the database table where the specified key column matches its value and any of the additional column-value pairs match.
- `Optional<List<Row>> getByKey(Column keyColumn, Object keyValue)`
  - Retrieves a list of all rows from the database table where the specified column matches the provided value.
- `Optional<Row> getFirstByKey(Column keyColumn, Object keyValue)`
  - Retrieves the first row from the database table where the specified column matches the provided value.
- `Optional<Object> getFirstColumnByKey(Column keyColumn, Object keyValue, Column returnColumn)`
  - Retrieves the value of the specified return column from the first row where the specified key column matches the provided value.
- `Optional<Row> getByOrder(Column orderColumn, Order order)`
  - Retrieves the first or last row from the database table, ordered by the specified column in ascending or descending order.
- `Optional<Row> getByOrderAndKey(Column keyColumn, Object keyValue, Column orderColumn, Order order)`
  - Retrieves the first or last row from the database table where the specified key column matches the provided value, ordered by the specified order column in ascending or descending order.
- `Optional<Row> getByOrderAndKeys(Column orderColumn, Order order, ColumnValue<Column>... keyPairs)`
  - Retrieves the first or last row from the database table where all specified column-value pairs match, ordered by the specified order column in ascending or descending order.
- `Optional<Row> getByKeys(ColumnValue<Column>... keyPairs)`
  - Retrieves the first row from the database table where all specified column-value pairs match.
- `Optional<List<Object>> getColumnByValue(Column keyColumn, Object keyValue, Column returnColumn)`
  - Retrieves a list of values from the specified return column for all rows where the specified key column matches the provided value.
- `Optional<Object> getColumnByOrder(Column keyColumn, Column returnColumn, Order order)`
  - Retrieves the value of the specified return column from the first or last row, ordered by the specified key column in ascending or descending order.
- `boolean update(Column keyColumn, Object keyValue, Column updateColumn, Object updateValue)`
  - Updates the specified column with the given update value for all rows where the specified key column matches the provided key value.
- `boolean updateByOrder(Column keyColumn, Order order, Column updateColumn, Object updateValue)`
  - Updates the specified column with the given update value for the first or last row, ordered by the specified key column in ascending or descending order.
- `boolean update(Row record)`
  - Updates the non-key columns of the specified row in the database table based on its key columns.
- `boolean upsert(Row record)`
  - Inserts a new row into the database table or updates an existing row if a duplicate key is found.

### Modification Methods
- `Boolean update(keyColumn, keyValue, updateColumn, updateValue)`
  - Updates a specific value
- `Boolean update(Row)`
  - Updates an entire row
- `Boolean updateByOrder(keyColumn, sortOrder, updateColumn, updateValue)`
  - Updates based on sorting
- `Boolean upsert(Row)`
  - Inserts a row
