# JavaSQL
# A Highly Customizable SQL Service

## JavaSQL Creates and maintains SQL Tables and makes it easy to use Data from the Database in Code.

### Built in Methods to manipulate and read Data from a Database Table:
- Integer: countByValue(keyColumn,keyValue)
  - Counts Rows with specified keyValue in keyColumn
- Boolean: existsByValue(keyColumn,keyValue)
  - Checks if min. one row with keyValue in keyColumn exists
- Optional<List<TableRow>>: getByValue(keyColumn,keyValue)
- Optional<TableRow>: getFirstByValue(keyColumn,keyValue)
- Optional<ColumnValue>: getFirstColumnByValue(keyColumn,keyValue,returnColumn)
- Optional<TableRow>: getByOrder(keyColumn,sortOrder)
- Optional<List<ColumnValue>>: getColumnByValue(keyColumn,keyValue,returnColumn)
- Optional<ColumnValue>: getColumnByOrder(keyColumn,returnColumn,sortOrder)
- ...
