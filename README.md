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
- `Integer countByValue(keyColumn, keyValue)`
  - Counts entries with a specific value
- `Boolean existsByValue(keyColumn, keyValue)`
  - Checks if a value exists
- `Optional<List<Row>> getByValue(keyColumn, keyValue)`
  - Retrieves all matching rows
- `Optional<Row> getFirstByValue(keyColumn, keyValue)`
  - Retrieves the first matching row
- `Optional<ColumnValue> getFirstColumnByValue(keyColumn, keyValue, returnColumn)`
  - Retrieves the first column value
- `Optional<Row> getByOrder(keyColumn, sortOrder)`
  - Retrieves a row based on sorting
- `Optional<List<ColumnValue>> getColumnByValue(keyColumn, keyValue, returnColumn)`
  - Retrieves column values
- `Optional<ColumnValue> getColumnByOrder(keyColumn, returnColumn, sortOrder)`
  - Retrieves a column value based on sorting

### Modification Methods
- `Boolean update(keyColumn, keyValue, updateColumn, updateValue)`
  - Updates a specific value
- `Boolean update(Row)`
  - Updates an entire row
- `Boolean updateByOrder(keyColumn, sortOrder, updateColumn, updateValue)`
  - Updates based on sorting
- `Boolean upsert(Row)`
  - Inserts a row
