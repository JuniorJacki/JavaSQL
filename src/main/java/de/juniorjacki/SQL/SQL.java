package de.juniorjacki.SQL;


import de.juniorjacki.SQL.Connection.SQLConnection;
import de.juniorjacki.SQL.Base.SQLDatabase;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

public class SQL extends SQLConnection implements SQLDatabase {



    private dbKey databaseKey;

    public static final SQL Service = new SQL();
    private Connection currentConnection = null;

    /**
     * @return Current SQL Connection. May Throw Exception if service is not started
     */
    public static Optional<Connection> getConnection() {
        return Optional.ofNullable(Service.getActiveConnection());
    }

    private static void updateConnection(Connection newConnection) {
        Service.currentConnection = newConnection;
    }

    private Connection getActiveConnection() {
        if (currentConnection != null) {
            try {
                if (checkConnection(currentConnection)) return currentConnection;
                closeConnection(currentConnection);
                currentConnection = null;
                getNewConnectionAsync(databaseKey, SQL::updateConnection);
                System.out.println("Try get New connection.");
                throw new RuntimeException("Database Offline");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Database Not Connected");
        // TODO Call Stop
    }

    public int start(dbKey databaseKey) {
        this.databaseKey = databaseKey;
        try {
            currentConnection = getNewConnection(databaseKey);
            if (currentConnection == null) return -1;
            if (!checkConnection(currentConnection)) return -2;
            buildDatabaseTables(currentConnection);
        } catch (SQLException e) {
            e.printStackTrace();
            return -3;
        }
        return 0;
    }

    public void stop() {
        stopAsyncConnectionGetter();
        if (currentConnection != null) {
            try {
                closeConnection(currentConnection);
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.out.println("Failed to close the database connection: " +e.getMessage());
            }
            currentConnection = null;
        }
    }
}
