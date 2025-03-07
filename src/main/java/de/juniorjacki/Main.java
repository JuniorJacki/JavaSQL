package de.juniorjacki;

import de.juniorjacki.SQL.Connection.SQLConnection;
import de.juniorjacki.SQL.Interface.DatabaseInterface;
import de.juniorjacki.SQL.SQL;
import de.juniorjacki.SQL.Structure.DataTable.ExampleTable;

import java.util.Optional;
import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        // Connect to Server, If Error connecting method returns Error Code
        // After Connection is established automatically creates missing Tables
        if (SQL.Service.start(new SQLConnection.dbKey("localhost",3316,"d1","username","password")) != 0) {
            System.out.println("Connection failed");
            return;
        }

        // Example Entry Insert- and Selection
        UUID uuid = UUID.randomUUID(); // Example Entry uID
        ExampleTable.Instance.upsert(new ExampleTable.Example(uuid,"Duck","Java","duck@java.de",17)); // Insert new Value into Database
        ExampleTable.Instance.getFirstByValue(ExampleTable.Property.uID,uuid).ifPresent(example -> {
                System.out.println(example.uID());
                System.out.println(example.preName());
                System.out.println(example.lastName());
                System.out.println(example.email());
                System.out.println(example.age());
            }
        );

        // Select first Row by Age Ascending
        ExampleTable.Instance.getByOrder(ExampleTable.Property.age, DatabaseInterface.Order.ASCENDING).ifPresent(example -> {
                    System.out.println(example.uID());
                    System.out.println(example.preName());
                    System.out.println(example.lastName());
                    System.out.println(example.email());
                    System.out.println(example.age());
                }
        );

        // Count of Entries with Age 17
        System.out.println(ExampleTable.Instance.countByValue(ExampleTable.Property.age, 17));

        // Names of Entries with Age 17
        ExampleTable.Instance.getColumnByValue(ExampleTable.Property.age, 17, ExampleTable.Property.preName).ifPresent(objects -> objects.forEach(System.out::println));

        // Select first Column by Age Ascending
        ExampleTable.Instance.getColumnByOrder(ExampleTable.Property.age, ExampleTable.Property.email,DatabaseInterface.Order.ASCENDING).ifPresent(System.out::println);

        // Update first Row by Age Ascending
        ExampleTable.Instance.updateByOrder(ExampleTable.Property.age, DatabaseInterface.Order.ASCENDING, ExampleTable.Property.lastName,"newLastName");

        // Update Row by Primary Keys in Record with Values of Record
        ExampleTable.Instance.update(new ExampleTable.Example(uuid,"DuckEdited","JavaEdited","editedduck@java.de",47));

        // Update Column from Rows with Specified Key
        ExampleTable.Instance.update(ExampleTable.Property.uID,uuid, ExampleTable.Property.age,57);

        // Stop Service
        SQL.Service.stop();
    }
}