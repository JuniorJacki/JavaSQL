package de.juniorjacki.SQL.Connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class SQLConnection {

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static Future<?> connectionTask;

    public record dbKey(String host, int port, String dataBase, String username, String passwd) {
    };

    protected boolean testKey(dbKey dbKey) throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://" + dbKey.host+":"+ dbKey.port + "/" + dbKey.dataBase, dbKey.username, dbKey.passwd).isValid(5000);
    }


    protected Connection getNewConnection(dbKey dbKey) throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://" + dbKey.host+":"+ dbKey.port + "/" + dbKey.dataBase, dbKey.username, dbKey.passwd);
    }

    protected void closeConnection(Connection con) throws SQLException {
        con.close();
    }

    protected boolean checkConnection(Connection dbConnection) throws SQLException {
        if (dbConnection != null) {
            return dbConnection.isValid(5000);
        }
        return false;
    }


    protected void getNewConnectionAsync(dbKey key,Consumer<Connection> connectionUpdater) {
        connectionTask = executorService.submit(() -> {
            while (running.get()) {
                try {
                    Connection conn = getNewConnection(key);
                    if (conn != null && conn.isValid(2)) {
                        synchronized (SQLConnection.class) {
                            connectionUpdater.accept(conn);
                            running.set(false);
                            System.out.println("Database reconnected successfully.");
                        }
                    }
                } catch (SQLException e) {
                    System.out.println("Failed to reconnect to the Database: " + e.getMessage());
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    protected static void stopAsyncConnectionGetter () {
        running.set(false);
        if (connectionTask != null) {
            connectionTask.cancel(true);
        }
        executorService.shutdown();
        System.out.println("Reconnect connection attempts stopped.");
    }
}







