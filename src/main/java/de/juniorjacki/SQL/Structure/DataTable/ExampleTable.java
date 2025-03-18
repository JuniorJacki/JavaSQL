package de.juniorjacki.SQL.Structure.DataTable;

import de.juniorjacki.SQL.Interface.DatabaseInterface;
import de.juniorjacki.SQL.Structure.DatabaseProperty;
import de.juniorjacki.SQL.Structure.DatabaseRecord;
import de.juniorjacki.SQL.Structure.Table;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ExampleTable extends Table<ExampleTable.Property,ExampleTable.Example> implements DatabaseInterface<ExampleTable,ExampleTable.Example,ExampleTable.Property> {
    @Override
    public Class<ExampleTable.Example> getTableRecord() {
        return ExampleTable.Example.class;
    }

    @Override
    public List<ExampleTable.Property> getProperties() {
        return Arrays.asList(ExampleTable.Property.values());
    }

    @Override
    public void onCreation() { // Code Gets Executed on Table Creation
        upsert(new ExampleTable.Example(UUID.randomUUID(),"Junior","Jacki","duck@juniorjacki.de",17));
    }


    @Override
    public ExampleTable getInstance() {
        return this;
    }

    public record Example(UUID uID,String preName,String lastName,String email,int age) implements DatabaseRecord<Example, Property> {
        @Override
        public ExampleTable.Example getInstance() {
            return this;
        }
    }

    public enum Property implements DatabaseProperty {

        uID(true, UUID.class), // Defines a Column named uID that is a Primary Key with the Datatype UUID -> VARCHAR(36)
        preName(false,String.class), // Defines a Column named preName with the Datatype String -> VARCHAR(255)
        lastName(false,String.class), // Defines a Column named lastName with the Datatype String -> VARCHAR(255)
        email(false,true,String.class), // Defines a Column named email that needs to be Unique with the Datatype String -> VARCHAR(255)
        age(false,Integer.class),; // Defines a Column named age with the Datatype Integer -> INT

        private final boolean key;
        private final boolean unique;
        private final Class<?> type;


        Property(boolean key, Class<?> type) {
            this.key = key;
            unique = false;
            this.type = type;
        }

        Property(boolean key,boolean unique, Class<?> type) {
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
        public Class<?> getType() {
            return type;
        }
    }
    public static ExampleTable Instance = new ExampleTable();


    // Custom Method
    public Optional<ExampleTable.Example> getLatestRecord() {
        return getByOrder(Property.uID,Order.DESCENDING);
    }
}