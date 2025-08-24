# JavaSQL - A Lightweight, Highly Customizable SQL Framework

The `de.juniorjacki.SQL.Interface` package provides a robust, type-safe framework for interacting with relational databases in Java. It combines the `DatabaseInterface` for basic CRUD operations and the `QueryBuilder` for constructing complex SQL queries with support for conditions, joins, grouping, ordering, and limits. The framework leverages Java's record types and generics to ensure type safety and streamline database interactions.

## Overview

This package offers two main interfaces:

- `DatabaseInterface`: Provides methods for common database operations such as retrieving, updating, deleting, and checking the existence of records, with built-in SQL injection protection and type validation.
- `QueryBuilder`: Enables the construction of complex SQL queries with a fluent API, supporting single-column, multi-column, and full-row queries, as well as table joins with type-safe bindings.

### Key Features

- **Type Safety**: Uses Java generics and enums to enforce type compatibility for columns and records.
- **Fluent API**: Method chaining for intuitive query construction.
- **Flexible Queries**: Supports single-column (`ColumnQuery`), multi-column (`ColumnsQuery`), full-row (`RowQuery`), and join queries (`BindingRowQuery`, `BindingColumnsQuery`).
- **Join Support**: Type-safe table joins using the `Binding` class to ensure column compatibility.
- **Condition Building**: `ConditionQueryBuilder` for constructing complex WHERE clauses with AND/OR conditions.
- **CRUD Operations**: Methods for counting, retrieving, updating, and deleting records with type validation and SQL injection filtering.
- **Custom Data Types**: Extensible `DatabaseType` enum for adding new data types with custom conversion logic.

This package is ideal for applications requiring dynamic SQL query generation and robust database operations with minimal boilerplate code.

## Installation

To use this package, include it in your Java project. Ensure the following dependencies are available:

- Java 17 or later (due to the use of records).
- JDBC-compatible database driver (e.g., MySQL, PostgreSQL).
- Project dependencies: `de.juniorjacki.SQL`, `de.juniorjacki.SQL.Structure`, `de.juniorjacki.SQL.Type`, `de.juniorjacki.SQL.Base`.

Clone the repository or include it as a dependency in your build tool (e.g., Maven or Gradle).

## Usage

### Defining a Table

To interact with a database table, create a class extending `Table` and implement `DatabaseInterface` and `QueryBuilder`. Define columns using an enum that implements `DatabaseProperty`.

```java
package de.juniorjacki.SQL.Structure.DataTable;

import de.juniorjacki.SQL.Interface.DatabaseInterface;
import de.juniorjacki.SQL.Interface.QueryBuilder;
import de.juniorjacki.SQL.Structure.DatabaseProperty;
import de.juniorjacki.SQL.Structure.Table;
import de.juniorjacki.SQL.Type.DatabaseRecord;
import de.juniorjacki.SQL.Type.DatabaseType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LicenseTable extends Table<LicenseTable.Property, LicenseTable.License> 
    implements DatabaseInterface<LicenseTable, LicenseTable.License, LicenseTable.Property>, 
               QueryBuilder<LicenseTable, LicenseTable.License, LicenseTable.Property> {

    @Override
    public Class<LicenseTable.License> getTableRecord() {
        return LicenseTable.License.class;
    }

    @Override
    public List<LicenseTable.Property> getProperties() {
        return Arrays.asList(LicenseTable.Property.values());
    }

    @Override
    public LicenseTable getInstance() {
        return this;
    }

    public record License(UUID uID, String value, Long creationTimestamp) 
        implements DatabaseRecord<LicenseTable.License, LicenseTable.Property> {
        @Override
        public LicenseTable.License getInstance() {
            return this;
        }
    }

    public enum Property implements DatabaseProperty {
        uID(true, DatabaseType.UUID), // Primary key, UUID -> BINARY(16)
        value(false, DatabaseType.STRING), // String -> VARCHAR(255)
        creationTimestamp(false, DatabaseType.LONG); // Long -> BIGINT

        private final boolean key;
        private final boolean unique;
        private final DatabaseType type;

        Property(boolean key, DatabaseType type) {
            this(key, false, type);
        }

        Property(boolean key, boolean unique, DatabaseType type) {
            this.key = key;
            this.unique = unique;
            this.type = type;
        }

        @Override
        public boolean isKey() {
            return key;
        }

        @Override
        public boolean isUnique() {
            return unique;
        }

        @Override
        public DatabaseType getType() {
            return type;
        }

        @Override
        public int extendLength() {
            return 0;
        }
    }

    public static LicenseTable Instance = new LicenseTable();

    // Custom method example
    public Optional<LicenseTable.License> getLatestRecord() {
        return getByOrder(LicenseTable.Property.uID, DatabaseInterface.Order.DESCENDING);
    }
}
```

#### Steps to Add a New Table

1. **Extend** `Table`:
   - Create a class extending `Table<E, R>`, where `E` is an enum implementing `DatabaseProperty` and `R` is a record implementing `DatabaseRecord`.
2. **Implement Interfaces**:
   - Implement `DatabaseInterface` and `QueryBuilder` to enable CRUD operations and query building.
3. **Define Columns**:
   - Create an enum implementing `DatabaseProperty` with columns, specifying their `DatabaseType`, key, and unique constraints.
4. **Define Record**:
   - Create a record implementing `DatabaseRecord` to represent table rows.
5. **Provide Singleton Instance**:
   - Create a static instance (e.g., `Instance`) for easy access.
6. **Override Methods**:
   - Implement `getTableRecord`, `getProperties`, and `getInstance` to define the table's metadata.
7. **Add Table to the Tables Enum**:
   - To Automatically create and maintain the Table, you need to add an Instance to the `Table Enum`.

### Basic CRUD Operations

Use `DatabaseInterface` methods to perform database operations:

```java
LicenseTable table = LicenseTable.Instance;

// Check if a record exists
boolean exists = table.existsByKey(LicenseTable.Property.uID, UUID.randomUUID());

// Count records
int count = table.countByValue(LicenseTable.Property.value, "license123");

// Retrieve a single record
Optional<LicenseTable.License> license = table.getFirstByKey(LicenseTable.Property.uID, UUID.randomUUID());

// Update a record
boolean updated = table.update(LicenseTable.Property.uID, UUID.randomUUID(), 
                              LicenseTable.Property.value, "newLicense");

// Upsert a record
LicenseTable.License record = new LicenseTable.License(UUID.randomUUID(), "license123", System.currentTimeMillis());
boolean upserted = table.upsert(record);

// Delete a record
boolean deleted = table.deleteByKeys(
    new DatabaseInterface.ColumnValue<>(LicenseTable.Property.uID, UUID.randomUUID())
);
```

### Building Queries

Use `QueryBuilder` for complex queries:

```java
// Single-column query
ColumnQuery<LicenseTable, LicenseTable.License, LicenseTable.Property> query = 
    table.newColumnQuery(LicenseTable.Property.value);
Optional<List<Object>> values = query.execute();

// Multi-column query
ColumnsQuery<LicenseTable, LicenseTable.License, LicenseTable.Property> columnsQuery = 
    table.newColumnsQuery(LicenseTable.Property.uID, LicenseTable.Property.value);
Optional<List<Map<LicenseTable.Property, Object>>> results = columnsQuery.execute();

// Row query with condition
ConditionQueryBuilder<LicenseTable.Property> condition = 
    new ConditionQueryBuilder<>(new Condition<>(LicenseTable.Property.value, 
        QueryBuilder.CompareOperator.EQUALS, "license123"));
RowQuery<LicenseTable, LicenseTable.License, LicenseTable.Property> rowQuery = 
    table.newRowQuery().setCondition(condition);
Optional<List<LicenseTable.License>> licenses = rowQuery.execute();
```

### Joining Tables

Join tables using type-safe bindings:

```java
// Define another table
class UserTable extends Table<UserTable.Property, UserTable.User> 
    implements DatabaseInterface<UserTable, UserTable.User, UserTable.Property>, 
               QueryBuilder<UserTable, UserTable.User, UserTable.Property> {
    // Similar structure to LicenseTable
    public enum Property implements DatabaseProperty {
        USER_ID(UUID.class, true),
        NAME(String.class, false);
        // Implementation
    }
    public record User(UUID userId, String name) implements DatabaseRecord<User, Property> {
        // Implementation
    }
    // Other required methods
}

// Join LicenseTable with UserTable
Binding<LicenseTable, LicenseTable.License, LicenseTable.Property, UserTable, UserTable.User, UserTable.Property> binding =
    new Binding<>(LicenseTable.Property.uID, UserTable.Property.USER_ID);
BindingRowQuery<LicenseTable, LicenseTable.License, LicenseTable.Property, UserTable, UserTable.User, UserTable.Property> joinQuery =
    table.newRowQuery().join(UserTable.INSTANCE, binding);
Optional<HashMap<LicenseTable.License, UserTable.User>> joinedResults = joinQuery.execute();
```

### Adding New Data Types

The `DatabaseType` enum in `de.juniorjacki.SQL.Type` defines supported database types and their conversion logic. To add a new data type (e.g., `FLOAT`):

1. **Add to** `DatabaseType` **Enum**:

   - Define a new enum value with the primary type, alias types, and conversion logic.

   - Example for `FLOAT`:

     ```java
     FLOAT(Float.class,
           List.of(float.class),
           (sb, value) -> sb.append((Float) value),
           ResultSet::getFloat,
           (ps, idx, val) -> ps.setFloat(idx, (Float) val),
           (extendedLength) -> "FLOAT")
     ```

2. **Parameters**:

   - **Primary Type**: The main Java class (e.g., `Float.class`).
   - **Alias Types**: Additional types (e.g., `float.class`) using `List.of`.
   - **Append Converter**: How to append the value to a `StringBuilder` for SQL queries.
   - **Result Set Converter**: How to retrieve the value from a `ResultSet` using `TriFunction`.
   - **Parameter Setter**: How to set the value in a `PreparedStatement` using `TriConsumer`.
   - **SQL Type Mapper**: Maps the type to an SQL type (e.g., `"FLOAT"`).

3. **Use in Table**:

   - Reference the new type in your table's `DatabaseProperty` enum:

     ```java
     public enum Property implements DatabaseProperty {
         TEMPERATURE(false, DatabaseType.FLOAT);
         // Other fields and methods
     }
     ```

#### Example

```java
FLOAT(Float.class,
      List.of(float.class),
      (sb, value) -> sb.append((Float) value),
      ResultSet::getFloat,
      (ps, idx, val) -> ps.setFloat(idx, (Float) val),
      (extendedLength) -> "FLOAT")
```

## Key Components

- `DatabaseInterface`: Provides CRUD operations like `getByKey`, `update`, `upsert`, `deleteByKeys`, and existence checks.
- `QueryBuilder`: Supports complex query construction with `ColumnQuery`, `ColumnsQuery`, `RowQuery`, `BindingRowQuery`, and `BindingColumnsQuery`.
- `Table`: Abstract base class for defining database tables.
- `DatabaseType`: Enum for mapping Java types to SQL types with conversion logic.
- `Binding`: Ensures type-safe joins between tables.
- `ConditionQueryBuilder`: Builds WHERE clauses with AND/OR conditions.
- `ColumnValue`: Represents key-value pairs for filtering.

## Error Handling

- **Input Validation**: Throws `InvalidParameterException` for invalid inputs (e.g., type mismatches).
- **SQL Injection Protection**: Uses `SQLInputFilter` to sanitize inputs.
- **Database Errors**: Wrapped in `RuntimeException` via `throwDBError`.
- **Type Mismatches**: Logged in `Binding` class, with invalid bindings set to null.

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/your-feature`).
3. Commit your changes (`git commit -m 'Add your feature'`).
4. Push to the branch (`git push origin feature/your-feature`).
5. Open a Pull Request.

## License

This project is licensed under the MIT License. See the LICENSE file for details.
