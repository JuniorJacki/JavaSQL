package de.juniorjacki.SQL.Base;



import de.juniorjacki.SQL.Structure.Table;
import de.juniorjacki.SQL.Structure.Tables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public interface SQLDatabase {

    /**
     * Creates Tables that are Referred in the @Tables.Class if not Exist
     * @param con
     * @return
     */
    default boolean buildDatabaseTables(Connection con) {
        try {
            for (Tables table : Tables.values()) {
                try {
                    Table<?,?> tableClass = table.instance;
                    con.createStatement().execute(SQLQueryBuilder.generateCreateTableQuery(tableClass.tableName(),tableClass.tableProperties()));
                    if (isTableEmpty(con, tableClass.tableName())) tableClass.onCreation();
                } catch (Exception exception) {
                    System.out.println("Table: "+table.name()+" Could not be created Successfully. Cause: "+exception.getMessage());
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Checks if a Table is Empty
     * @param conn Current Database Connection
     * @param tableName Table name to check if empty
     * @return If is Empty True, if not are an Error occurred False
     */
    private static boolean isTableEmpty(Connection conn, String tableName) {
        try (PreparedStatement prepStatement = conn.prepareStatement("SELECT COUNT(*) FROM " + tableName);
             ResultSet rs = prepStatement.executeQuery()) {
            if (rs.next()) return rs.getInt(1) == 0;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
