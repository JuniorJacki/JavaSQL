package de.juniorjacki.SQL.Query;

import de.juniorjacki.SQL.Structure.Table;
import java.util.ArrayList;
import java.util.List;

public class SQLQueryBuilder {


    public static String generateCreateTableQuery(String tableName, List<Table.Property> properties) {
        StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        query.append(tableName);
        query.append(" (");

        List<String> primaryKeys = new ArrayList<>();

        for (int i = 0; i < properties.size(); i++) {
            Table.Property property = properties.get(i);
            query.append(property.dbName())
                    .append(" ")
                    .append(getSQLType(property.dataType()));
            if (property.unique() && !property.key()) {
                query.append(" UNIQUE");
            }
            if (property.key()) {
                primaryKeys.add(property.dbName());
            }

            if (i < properties.size() - 1) {
                query.append(", ");
            }
        }

        if (!primaryKeys.isEmpty()) {
            query.append(", PRIMARY KEY (");
            query.append(String.join(", ", primaryKeys));
            query.append(")");
        }

        query.append(");");
        return query.toString();
    }


    private static String getSQLType(Class<?> dataType) {
        return switch (dataType.getSimpleName()) {
            case "UUID" -> "VARCHAR(36)";
            case "String" -> "VARCHAR(255)";
            case "JSONObject" -> "JSON";
            case "Long", "long" -> "BIGINT";
            case "Integer" -> "INT";
            case "Boolean" -> "BOOLEAN";
            case "Double" -> "DOUBLE";
            default -> "UNKNOWN";
        };
    }


}
