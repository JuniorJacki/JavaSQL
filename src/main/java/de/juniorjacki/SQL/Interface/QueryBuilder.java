package de.juniorjacki.SQL.Interface;

import de.juniorjacki.SQL.SQL;
import de.juniorjacki.SQL.Structure.DatabaseProperty;
import de.juniorjacki.SQL.Structure.Table;
import de.juniorjacki.SQL.Type.DatabaseRecord;
import de.juniorjacki.SQL.Type.Record;

import java.util.*;
import java.util.stream.Collectors;

import static de.juniorjacki.SQL.Interface.InterDefinitions.*;
import static de.juniorjacki.SQL.Interface.InterDefinitions.appendParameter;

public interface QueryBuilder<G extends Table<E, R>, R extends java.lang.Record & DatabaseRecord<R, E>, E extends Enum<E> & DatabaseProperty> {

    G getInstance();

    /**
     * @param returnColumn Column the Query should request from Database
     */
    default ColumnQuery<G, R, E> newColumnQuery(E returnColumn) {
        return new ColumnQuery<>(getInstance(), returnColumn);
    }

    default RowQuery<G, R, E> newRowQuery() {
        return new RowQuery<>(getInstance());
    }

    /**
     * @param returnColumn Columns the Query should request from Database
     */
    default ColumnsQuery<G, R, E> newColumnsQuery(E... returnColumn) {
        return new ColumnsQuery<>(getInstance(), returnColumn);
    }


    /**
     * Abstract base class for building SQL queries with conditions, grouping, ordering, and limits.
     *
     * @param <T> The type of the query class (for method chaining)
     * @param <S> The Table used for Queries
     * @param <E> The enum type representing database properties, extending DatabaseProperty
     */
    public abstract class Query<T,S extends Table, E extends Enum<E> & DatabaseProperty> {
        protected ConditionQueryBuilder<E> conditionQuery;
        protected int limit = -1;
        protected E orderBy;
        protected DatabaseInterface.Order order;
        protected E groupBy;
        protected final S table;

        /**
         * Constructs a Query with the specified table.
         *
         * @param table The table to query
         * @throws IllegalArgumentException if the table is null
         */
        protected Query(S table) {
            if (table == null) {
                throw new IllegalArgumentException("Table cannot be null");
            }
            this.table = table;
        }

        /**
         * Sets the conditions for the data requested from the database.
         * This allows filtering the results based on specified conditions.
         *
         * @param conditionQuery The condition query builder containing the conditions
         * @return The current query instance for method chaining
         * @throws IllegalArgumentException if the conditionQuery is null
         */
        @SuppressWarnings("unchecked")
        public T setCondition(ConditionQueryBuilder<E> conditionQuery) {
            if (conditionQuery == null) {
                throw new IllegalArgumentException("Condition query cannot be null");
            }
            this.conditionQuery = conditionQuery;
            return (T) this;
        }

        /**
         * Limits the number of rows returned by the query.
         *
         * @param limit The maximum number of rows to return
         * @return The current query instance for method chaining
         * @throws IllegalArgumentException if the limit is negative
         */
        @SuppressWarnings("unchecked")
        public T limitBy(int limit) {
            this.limit = limit;
            return (T) this;
        }

        /**
         * Sets the sorting order for the query results.
         *
         * @param orderBy The database property to sort by
         * @param order The sort direction (ASC or DESC)
         * @return The current query instance for method chaining
         * @throws IllegalArgumentException if orderBy or order is null
         */
        @SuppressWarnings("unchecked")
        public T orderBy(E orderBy, DatabaseInterface.Order order) {
            if (orderBy == null || order == null) {
                throw new IllegalArgumentException("OrderBy and order cannot be null");
            }
            this.orderBy = orderBy;
            this.order = order;
            return (T) this;
        }

        /**
         * Sets the grouping for the query results.
         *
         * @param groupBy The database property to group by
         * @return The current query instance for method chaining
         * @throws IllegalArgumentException if groupBy is null
         */
        @SuppressWarnings("unchecked")
        public T groupBy(E groupBy) {
            if (groupBy == null) {
                throw new IllegalArgumentException("GroupBy cannot be null");
            }
            this.groupBy = groupBy;
            return (T) this;
        }

        /**
         * Builds the base SQL query string with the specified select clause.
         * This method constructs the query including conditions, grouping, ordering, and limits.
         *
         * @param selectClause The SQL SELECT clause (e.g., "*" or "COUNT(*)")
         * @return A StringBuilder containing the constructed SQL query
         */
        protected StringBuilder buildQueryBase(String selectClause) {
            StringBuilder query = new StringBuilder("SELECT ").append(selectClause)
                    .append(" FROM ").append(table.tableName()).append(" ");
            if (conditionQuery != null) {
                query.append(conditionQuery.build());
            }
            if (groupBy != null) {
                query.append(" GROUP BY ").append(groupBy.name());
            }
            if (orderBy != null) {
                query.append(" ORDER BY ").append(orderBy.name()).append(" ").append(order.sql);
            }
            if (limit < 0) {
                query.append(" LIMIT ").append(limit);
            }
            return query;
        }

        /**
         * Checks if at least one row exists for the query on the associated table.
         *
         * @return true if at least one row exists, false otherwise
         */
        public boolean exists() {
            int cLimit = limit;
            try {
                limitBy(1);
                String query = buildQueryBase("1").toString();
                try (var prepStatement = SQL.getConnection().get().prepareStatement(query);
                     var rs = prepStatement.executeQuery()) {
                    return rs.next();
                } catch (Exception e) {
                    throwDBError(e);
                    return false;
                }
            } finally {
                limitBy(cLimit);
            }
        }

        /**
         * Counts the number of rows that match the query for the associated table.
         *
         * @return The number of matching rows as a long
         */
        public long count() {
            String query = buildQueryBase("COUNT(*)").toString();
            try (var prepStatement = SQL.getConnection().get().prepareStatement(query);
                 var rs = prepStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            } catch (Exception e) {
                throwDBError(e);
                return 0L;
            }
        }


    }

    class RowQuery<G extends Table<E, R>, R extends java.lang.Record & DatabaseRecord<R, E>, E extends Enum<E> & DatabaseProperty> extends Query<RowQuery<G,R,E>,G, E> {

        /**
         * Constructs a RowQuery for the specified table.
         *
         * @param table The table to query
         * @throws IllegalArgumentException if the table is null
         */
        public RowQuery(G table) {
            super(table);
        }

        /**
         * Creates a BindingRowQuery to join the current table with another table.
         *
         * @param <U> The type of the table to join with
         * @param <A> The record type of the joined table
         * @param <I> The enum type representing properties of the joined table
         * @param joinTable The table to join with
         * @param bindings The bindings defining the join conditions
         * @return A new BindingRowQuery instance
         */
        public <U extends Table<I, A>, A extends java.lang.Record & DatabaseRecord<A, I>, I extends Enum<I> & DatabaseProperty> BindingRowQuery<G, R, E, U, A, I> join(U joinTable, Binding<G, R, E, U, A, I>... bindings) {
            return new BindingRowQuery<G, R, E, U, A, I>((G) table, buildQueryBase("*"), joinTable, bindings);
        }


        /**
         * Executes the query and returns all matching rows as a list of records.
         *
         * @return An Optional containing the list of records, or empty if an error occurs
         */
        public Optional<List<R>> execute() {
            String query = buildQueryBase("*").toString();
            try (var prepStatement = SQL.getConnection().get().prepareStatement(query);
                 var rs = prepStatement.executeQuery()) {
                List<R> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add((R) Record.populateRecord(table, rs));
                }
                return Optional.ofNullable(rows.isEmpty() ? null : rows);
            } catch (Exception e) {
                throwDBError(e);
                return Optional.empty();
            }
        }

        /**
         * Executes the query and returns the first matching row.
         *
         * @return An Optional containing the first record, or empty if no rows exist or an error occurs
         */
        public Optional<R> executeOneRow() {
            int cLimit = limit;
            try {
                this.limitBy(1);
                return execute().map(List::getFirst);
            } finally {
                this.limitBy(cLimit);
            }
        }
    }

    /**
     * A query class for retrieving specific columns from 2 tables.
     * This class extends Query to support joins between a reference table and a binding table,
     * selecting only the specified columns from each table.
     *
     * @param <G> The type of the reference table
     * @param <R> The record type associated with the reference table
     * @param <E> The enum type representing properties of the reference table
     * @param <U> The type of the binding table
     * @param <A> The record type associated with the binding table
     * @param <I> The enum type representing properties of the binding table
     */
    class BindingColumnsQuery<G extends Table<E, R>, R extends java.lang.Record & DatabaseRecord<R, E>, E extends Enum<E> & DatabaseProperty,U extends Table<I, A>, A extends java.lang.Record & DatabaseRecord<A, I>, I extends Enum<I> & DatabaseProperty> extends Query<BindingRowQuery<G,R,E,U,A,I>,U, I> {
        private final Table<?, ?> refTable;
        private final StringBuilder refTableQueryBase;
        private final Table<?, ?> bindingTable;
        private final List<Binding<G,R,E,U,A,I>> bindings;
        private final Set<E> refColumns;
        private final Set<I> bindingColumns;


        /**
         * Constructs a BindingColumnsQuery for joining two tables and selecting specific columns.
         *
         * @param current The reference table
         * @param refTableQueryBase The base query for the reference table (as a subquery)
         * @param bindingTable The table to join with
         * @param refColumns The columns to select from the reference table
         * @param bindingColumns The columns to select from the binding table
         * @param bindings The bindings defining the join conditions
         * @throws IllegalArgumentException if any parameter is null or if bindings are empty
         */
        public BindingColumnsQuery(G current, StringBuilder refTableQueryBase, U bindingTable, HashSet<E> refColumns, HashSet<I> bindingColumns, Binding<G,R,E,U,A,I>... bindings) {
            super(bindingTable);
            if (current == null || refTableQueryBase == null || bindingTable == null || refColumns == null || bindingColumns == null) {
                throw new IllegalArgumentException("Parameters cannot be null");
            }
            if (bindings.length == 0) {
                throw new IllegalArgumentException("At least one binding is required");
            }
            this.refTable = current;
            this.refTableQueryBase = refTableQueryBase;
            this.bindingTable = bindingTable;
            this.refColumns = refColumns;
            this.bindingColumns =bindingColumns;
            this.bindings = Arrays.asList(bindings);
        }

        /**
         * Builds the base SQL query string for joining the reference and binding tables,
         * selecting only the specified columns.
         *
         * @param selectClause The SQL SELECT clause (ignored, as columns are predefined)
         * @return A StringBuilder containing the constructed SQL query
         * @throws IllegalStateException if no valid bindings are present
         */
        @Override
        protected StringBuilder buildQueryBase(String selectClause) {
            StringBuilder query = new StringBuilder("SELECT ");
            List<CharSequence> columns = new ArrayList<>();
            columns.addAll(refColumns.stream()
                    .map(p -> refTable.tableName() + "." + p.name())
                    .toList());
            columns.addAll(bindingColumns.stream()
                    .map(p -> bindingTable.tableName() + "." + p.name())
                    .toList());
            if (columns.isEmpty()) {
                throw new IllegalStateException("No columns specified for selection");
            }
            query.append(String.join(", ", columns));

            // FROM clause with refTableQueryBase as subquery and INNER JOIN
            query.append(" FROM (").append(refTableQueryBase).append(") AS ")
                    .append(refTable.tableName())
                    .append(" INNER JOIN ").append(bindingTable.tableName())
                    .append(" ON ");

            // Join conditions
            String joinConditions = bindings.stream()
                    .filter(b -> b.getRef() != null && b.getJoinRef() != null)
                    .map(b -> refTable.tableName() + "." + b.getRef().name() + " = " +
                            bindingTable.tableName() + "." + b.getJoinRef().name())
                    .collect(Collectors.joining(" AND "));
            if (joinConditions.isEmpty()) {
                throw new IllegalStateException("No valid bindings for the join");
            }
            query.append(joinConditions);

            // Additional conditions
            if (conditionQuery != null) {
                query.append(" WHERE ").append(conditionQuery.build());
            }
            if (groupBy != null) {
                query.append(" GROUP BY ").append(groupBy.name());
            }
            if (orderBy != null) {
                query.append(" ORDER BY ").append(orderBy.name()).append(" ").append(order.sql);
            }
            if (limit != -1) {
                query.append(" LIMIT ").append(limit);
            }

            return query;
        }

        /**
         * Executes the query and returns a map of column values from the reference table
         * to column values from the binding table.
         *
         * @return An Optional containing a HashMap mapping reference table column values
         *         to binding table column values, or empty if an error occurs
         */
        public Optional<HashMap<HashMap<E, Object>, HashMap<I, Object>>> execute() {
            String query = buildQueryBase("*").toString();
            try (var prepStatement = SQL.getConnection().get().prepareStatement(query);
                 var rs = prepStatement.executeQuery()) {
                HashMap<HashMap<E, Object>, HashMap<I, Object>> result = new HashMap<>();
                while (rs.next()) {
                    HashMap<E, Object> refRow = new HashMap<>();
                    for (E column : refColumns) {
                        refRow.put(column, InterDefinitions.getTypedValue(rs, column, refTable));
                    }
                    HashMap<I, Object> bindingRow = new HashMap<>();
                    for (I column : bindingColumns) {
                        bindingRow.put(column, InterDefinitions.getTypedValue(rs, column, bindingTable));
                    }
                    result.put(refRow, bindingRow);
                }
                return Optional.ofNullable(result.isEmpty() ? null : result);
            } catch (Exception e) {
                throwDBError(e);
                return Optional.empty();
            }
        }

        /**
         * Executes the query and returns the first matching row as a map entry.
         *
         * @return An Optional containing the first map entry of reference table column values
         *         to binding table column values, or empty if no rows exist or an error occurs
         */
        public Optional<Map.Entry<HashMap<E, Object>, HashMap<I, Object>>> executeOneRow() {
            int cLimit = limit;
            try {
                this.limitBy(1);
                return execute().filter(map -> !map.isEmpty()).map(map -> map.entrySet().iterator().next());
            } finally {
                this.limitBy(cLimit);
            }
        }
    }

    /**
     * A query class for retrieving entire rows from two joined tables.
     * This class extends Query to support joins between a reference table and a binding table,
     * returning complete records from both tables.
     *
     * @param <G> The type of the reference table
     * @param <R> The record type associated with the reference table
     * @param <E> The enum type representing properties of the reference table
     * @param <U> The type of the binding table
     * @param <A> The record type associated with the binding table
     * @param <I> The enum type representing properties of the binding table
     */
    class BindingRowQuery<G extends Table<E, R>, R extends java.lang.Record & DatabaseRecord<R, E>, E extends Enum<E> & DatabaseProperty,U extends Table<I, A>, A extends java.lang.Record & DatabaseRecord<A, I>, I extends Enum<I> & DatabaseProperty> extends Query<BindingRowQuery<G,R,E,U,A,I>, U,I> {
        private final G refTable;
        private final StringBuilder refTableQueryBase;
        private final List<Binding<G, R, E, U, A, I>> bindings;

        /**
         * Constructs a BindingRowQuery for joining two tables.
         *
         * @param current The reference table
         * @param refTableQueryBase The base query for the reference table (as a subquery)
         * @param bindingTable The table to join with
         * @param bindings The bindings defining the join conditions
         * @throws IllegalArgumentException if any parameter is null or if bindings are empty
         */
        public BindingRowQuery(G current, StringBuilder refTableQueryBase, U bindingTable, Binding<G, R, E, U, A, I>... bindings) {
            super(bindingTable);
            if (current == null || refTableQueryBase == null || bindingTable == null) {
                throw new IllegalArgumentException("Parameters cannot be null");
            }
            if (bindings.length == 0) {
                throw new IllegalArgumentException("At least one binding is required");
            }
            this.refTable = current;
            this.refTableQueryBase = refTableQueryBase;
            this.bindings = List.of(bindings); // Immutable list for efficiency
        }

        /**
         * Builds the base SQL query string for joining the reference and binding tables,
         * selecting all columns from both tables.
         *
         * @param selectClause The SQL SELECT clause (ignored, as all columns are selected)
         * @return A StringBuilder containing the constructed SQL query
         * @throws IllegalStateException if no valid bindings are present
         */
        @Override
        protected StringBuilder buildQueryBase(String selectClause) {
            StringBuilder query = new StringBuilder("SELECT ");
            // Combine column names from refTable and bindingTable
            List<CharSequence> columns = new ArrayList<>();
            columns.addAll(refTable.getProperties().stream()
                    .map(p -> refTable.tableName() + "." + p.name())
                    .toList());
            columns.addAll(this.table.getProperties().stream()
                    .map(p -> this.table.tableName() + "." + p.name())
                    .toList());
            if (columns.isEmpty()) {
                throw new IllegalStateException("No columns available for selection");
            }
            query.append(String.join(", ", columns));

            // FROM clause with refTableQueryBase as subquery and INNER JOIN
            query.append(" FROM (").append(refTableQueryBase).append(") AS ")
                    .append(refTable.tableName())
                    .append(" INNER JOIN ").append(this.table.tableName())
                    .append(" ON ");

            // Join conditions
            String joinConditions = bindings.stream()
                    .filter(b -> b.getRef() != null && b.getJoinRef() != null)
                    .map(b -> refTable.tableName() + "." + b.getRef().name() + " = " +
                            this.table.tableName() + "." + b.getJoinRef().name())
                    .collect(Collectors.joining(" AND "));
            if (joinConditions.isEmpty()) {
                throw new IllegalStateException("No valid bindings for the join");
            }
            query.append(joinConditions);

            // Additional conditions
            if (conditionQuery != null) {
                query.append(" WHERE ").append(conditionQuery.build());
            }
            if (groupBy != null) {
                query.append(" GROUP BY ").append(groupBy.name());
            }
            if (orderBy != null) {
                query.append(" ORDER BY ").append(orderBy.name()).append(" ").append(order.sql);
            }
            if (limit != -1) {
                query.append(" LIMIT ").append(limit);
            }

            return query;
        }

        /**
         * Executes the query and returns a map of reference table records to binding table records.
         *
         * @return An Optional containing a HashMap mapping reference table records to binding table records,
         *         or empty if an error occurs
         */
        public Optional<HashMap<R, A>> execute() {
            String query = buildQueryBase("*").toString();
            try (var prepStatement = SQL.getConnection().get().prepareStatement(query);
                 var rs = prepStatement.executeQuery()) {
                HashMap<R, A> rows = new HashMap<>();
                while (rs.next()) {
                    rows.put((R)Record.populateRecord(refTable, rs,true),(A)Record.populateRecord(this.table, rs,true));
                }
                return Optional.ofNullable(rows.isEmpty() ? null : rows);
            } catch (Exception e) {
                throwDBError(e);
                return Optional.empty();
            }
        }

        /**
         * Executes the query and returns the first matching row as a map entry.
         * Optimizes the query by directly applying LIMIT 1.
         *
         * @return An Optional containing the first map entry of reference table record to binding table record,
         *         or empty if no rows exist or an error occurs
         */
        public Optional<Map.Entry<R, A>> executeOneRow() {
            int cLimit = limit;
            try {
                this.limitBy(1);
                return execute().filter(map -> !map.isEmpty()).map(map -> map.entrySet().iterator().next());
            } finally {
                this.limitBy(cLimit);
            }
        }
    }


    /**
     * A query class for retrieving a single column from a table.
     * This class extends Query to support selecting a specific column with optional conditions,
     * grouping, ordering, and limits.
     *
     * @param <G> The type of the table
     * @param <R> The record type associated with the table
     * @param <E> The enum type representing properties of the table
     */
    class ColumnQuery<G extends Table<E, R>, R extends java.lang.Record & DatabaseRecord<R, E>, E extends Enum<E> & DatabaseProperty> extends Query<ColumnQuery<G, R, E>,G, E> {
        private final E returnColumn;

        /**
         * Constructs a ColumnQuery for selecting a specific column from a table.
         *
         * @param table The table to query
         * @param returnColumn The column to select
         */
        public ColumnQuery(G table, E returnColumn) {
            super(table);
            this.returnColumn = returnColumn;
        }


        /**
         * Creates a BindingColumnsQuery to join the current table with another table,
         * selecting specific columns from both tables.
         *
         * @param <U> The type of the table to join with
         * @param <A> The record type of the joined table
         * @param <I> The enum type representing properties of the joined table
         * @param joinTable The table to join with
         * @param resultColumns The columns to select from the joined table
         * @param bindings The bindings defining the join conditions
         * @return A new BindingColumnsQuery instance
         * @throws IllegalArgumentException if joinTable or joinResultColumns is null, or if bindings are empty
         */
        public <U extends Table<I, A>, A extends java.lang.Record & DatabaseRecord<A, I>, I extends Enum<I> & DatabaseProperty> BindingColumnsQuery<G, R, E, U, A, I> join(U joinTable, ResultColumns<I> resultColumns, Binding<G, R, E, U, A, I>... bindings) {
            if (bindings.length == 0) {
                throw new IllegalArgumentException("At least one binding is required");
            }
            return new BindingColumnsQuery<>(table, buildQueryBase(returnColumn.name()), joinTable, new HashSet<>(Arrays.asList(returnColumn)),resultColumns.getHashSet(), bindings);
        }

        /**
         * Executes the query and returns a list of values for the selected column.
         *
         * @return An Optional containing a list of values for the selected column,
         *         or empty if an error occurs
         */
        public Optional<List<Object>> execute() {
            String query = buildQueryBase(returnColumn.name()).toString();
            System.out.println(query);
            try (var prepStatement = SQL.getConnection().get().prepareStatement(query);
                 var rs = prepStatement.executeQuery()) {
                List<Object> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(DatabaseInterface.getTypedValue(rs, returnColumn));
                }
                return Optional.ofNullable(results.isEmpty() ? null : results);
            } catch (Exception e) {
                throwDBError(e);
                return Optional.empty();
            }
        }


        /**
         * Executes the query and returns the first value of the selected column.
         *
         * @return An Optional containing the first value of the selected column,
         *         or empty if no rows exist or an error occurs
         */
        public Optional<Object> executeOne() {
            int cLimit = limit;
            try {
                this.limitBy(1);
                return execute().map(List::getFirst);
            } finally {
                this.limitBy(cLimit);
            }
        }
    }

    /**
     * A utility class to encapsulate a set of columns to be selected from a table.
     *
     * @param <I> The enum type representing properties of the table
     */
    class ResultColumns<I> {
        HashSet<I> resultColumns;

        /**
         * Constructs a ResultColumns instance with the specified columns.
         *
         * @param resultColumns The columns to include
         */
        @SafeVarargs
        public ResultColumns(I... resultColumns) {
            this.resultColumns = resultColumns == null ? null : new HashSet<>(Arrays.asList(resultColumns));
        }

        /**
         * Returns the immutable set of columns.
         *
         * @return An immutable Set of columns
         */
        public HashSet<I> getHashSet() {
            return resultColumns;
        }
    }


    class ColumnsQuery<G extends Table<E, R>, R extends java.lang.Record & DatabaseRecord<R, E>, E extends Enum<E> & DatabaseProperty>
            extends Query<ColumnsQuery<G, R, E>,G, E> {
        private final Set<E> returnColumns;

        /**
         * Constructs a ColumnsQuery for selecting specific columns from a table.
         *
         * @param table The table to query
         * @param returnColumns The columns to select
         * @throws IllegalArgumentException if table or returnColumns is null, contains null elements, or contains invalid properties
         */
        @SafeVarargs
        public ColumnsQuery(G table, E... returnColumns) {
            super(table);
            if (returnColumns == null) {
                throw new IllegalArgumentException("Return columns cannot be null");
            }
            if (Arrays.stream(returnColumns).anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Return columns cannot contain null elements");
            }
            this.returnColumns = Set.of(returnColumns);
        }


        /**
         * Creates a BindingColumnsQuery to join the current table with another table,
         * selecting specific columns from both tables.
         *
         * @param <U> The type of the table to join with
         * @param <A> The record type of the joined table
         * @param <I> The enum type representing properties of the joined table
         * @param joinTable The table to join with
         * @param resultColumns The columns to select from the joined table
         * @param bindings The bindings defining the join conditions
         * @return A new BindingColumnsQuery instance
         */
        public <U extends Table<I, A>, A extends java.lang.Record & DatabaseRecord<A, I>, I extends Enum<I> & DatabaseProperty> BindingColumnsQuery<G, R, E, U, A, I> join(U joinTable, ResultColumns<I> resultColumns, Binding<G, R, E, U, A, I>... bindings) {
            if (bindings.length == 0) {
                throw new IllegalArgumentException("At least one binding is required");
            }
            return new BindingColumnsQuery<>(table, buildQueryBase(String.join(",", returnColumns.stream().map(E::name).toArray(String[]::new))), joinTable, new HashSet<E>(returnColumns),resultColumns.getHashSet(), bindings);
        }


        /**
         * Executes the query and returns a list of maps containing the selected column values.
         *
         * @return An Optional containing a list of maps with column values,
         *         or empty if an error occurs
         */
        public Optional<List<Map<E, Object>>> execute() {
            String query = buildQueryBase(String.join(",", returnColumns.stream().map(E::name).toArray(String[]::new))).toString();
            try (var prepStatement = SQL.getConnection().get().prepareStatement(query);
                 var rs = prepStatement.executeQuery()) {
                List<Map<E, Object>> results = new ArrayList<>();
                while (rs.next()) {
                    Map<E, Object> row = new HashMap<>();
                    for (E column : returnColumns) {
                        row.put(column, DatabaseInterface.getTypedValue(rs, column));
                    }
                    results.add(row);
                }
                return Optional.ofNullable(results.isEmpty() ? null : results);
            } catch (Exception e) {
                throwDBError(e);
                return Optional.empty();
            }
        }

        /**
         * Executes the query and returns the first row of selected column values.
         * Optimizes the query by directly applying LIMIT 1.
         *
         * @return An Optional containing a map with the first row's column values,
         *         or empty if no rows exist or an error occurs
         */
        public Optional<Map<E, Object>> executeOne() {
            int cLimit = limit;
            try {
                this.limitBy(1);
                return execute().map(List::getFirst);
            } finally {
                this.limitBy(cLimit);
            }
        }
    }


     class ConditionQueryBuilder<E extends Enum<E> & DatabaseProperty> implements Cloneable {
        Condition<E> initalCondition;
        Queue<Map.Entry<Condition<E>,ConditionType>> conditions = new LinkedList<>();

        public ConditionQueryBuilder(Condition<E> initalCondition) {
            this.initalCondition = initalCondition;
        }

        public ConditionQueryBuilder<E> AND(Condition<E> additionalCondition) {
            ConditionQueryBuilder<E> updated = clone();
            updated.conditions.add(new AbstractMap.SimpleEntry<Condition<E>,ConditionType>(additionalCondition, ConditionType.AND));
            return updated;
        }

        public ConditionQueryBuilder<E> OR(Condition<E> additionalCondition) {
            ConditionQueryBuilder<E> updated = clone();
            updated.conditions.add(new AbstractMap.SimpleEntry<Condition<E>,ConditionType>(additionalCondition, ConditionType.OR));
            return updated;
        }

        protected StringBuilder build() {
            if (initalCondition == null) return new StringBuilder();
            StringBuilder query = new StringBuilder();
            if (initalCondition.typeCheck()) {
                query = appendCondition(query,initalCondition,null);
                Map.Entry<Condition<E>,ConditionType> condition;
                while((condition = conditions.poll()) != null) {
                    query = appendCondition(query,condition.getKey(),condition.getValue());
                }
                query.insert(0, "WHERE ");
                return query;
            } else {
                initalCondition.throwInputError();
                return null;
            }
        }

        private StringBuilder appendCondition(StringBuilder currentQuery,Condition condition,ConditionType conditionType) {
            StringBuilder conditionQuery = new StringBuilder();
            conditionQuery.append(condition.column.name());
            conditionQuery.append(condition.operator.sql);
            appendParameter(conditionQuery,condition.value);

            if (conditionType != null) {
                switch (conditionType) {
                    case AND -> {
                        currentQuery.append(" AND ");
                        currentQuery.append(conditionQuery);
                    }
                    case OR ->  {
                        currentQuery.insert(0," (");
                        currentQuery.append(") ");
                        currentQuery.append(" OR ");
                        currentQuery.append(conditionQuery);
                    }
                }
            } else {
                currentQuery.append(conditionQuery);
            }
            return currentQuery;
        }

        @Override
        protected ConditionQueryBuilder<E> clone() {
            try {
                return (ConditionQueryBuilder<E>) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        enum ConditionType {
            AND,
            OR
        }
    }

    /**
     * Represents a binding between two database table columns, ensuring type compatibility.
     * This class links a column from a primary table to a column in a joined table,
     * verifying that both columns have the same data type.
     *
     * @param <G> The type of the primary table, extending {@link Table}.
     * @param <R> The type of the record for the primary table, extending {@link Record} and {@link DatabaseRecord}.
     * @param <E> The enum type for the primary table's properties, implementing {@link DatabaseProperty}.
     * @param <U> The type of the joined table, extending {@link Table}.
     * @param <A> The type of the record for the joined table, extending {@link Record} and {@link DatabaseRecord}.
     * @param <I> The enum type for the joined table's properties, implementing {@link DatabaseProperty}.
     */
    class Binding<G extends Table<E, R>, R extends java.lang.Record & DatabaseRecord<R, E>, E extends Enum<E> & DatabaseProperty,U extends Table<I, A>, A extends java.lang.Record & DatabaseRecord<A, I>, I extends Enum<I> & DatabaseProperty> {
        E ref;
        I joinref;

        /**
         * Constructs a new Binding between a primary table column and a joined table column.
         * Checks if the data types of both columns match. If they do not match, the query will fail
         *
         * @param ref     The column from the primary table.
         * @param joinref The column from the joined table.
         */
        public Binding(E ref, I joinref) {
            if (ref.getType() != joinref.getType()) {
                System.out.println("Type mismatch for Binding between "+ref.name()+":"+ref.getType()+ " " +joinref.name()+":"+joinref.getType());
                this.ref = null;
                this.joinref = null;
                return;
            }
            this.ref = ref;
            this.joinref = joinref;
        }

        public E getRef() {
            return ref;
        }

        public I getJoinRef() {
            return joinref;
        }
    }


    class Condition<E extends Enum<E> & DatabaseProperty> {
        public E column;
        public Object value;
        public CompareOperator operator;

        public void throwInputError() {
            System.out.println("Invalid value type for " + column.name() + ": " + value.getClass().getName() +", expected: " + column.getType().name());
        }

        public boolean typeCheck() {
            if (column.getType().getTypeClass().isInstance(value)) {
                if (operator != CompareOperator.EQUALS) {
                    return column.getType().getTypeClass() != String.class;
                }
                return true;
            }
            return false;

        }

        public Condition(E conditionColumn, CompareOperator comparisonOperator, Object conditionValue) {
            this.column = conditionColumn;
            this.value = conditionValue;
            this.operator = comparisonOperator;
        }

    }



    private static void throwDBError(Exception e){
        e.printStackTrace();
    }


}
