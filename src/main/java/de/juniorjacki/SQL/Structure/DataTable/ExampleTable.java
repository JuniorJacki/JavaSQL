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

        uID(true, UUID.class),
        preName(false,String.class),
        lastName(false,String.class),
        email(false,String.class),
        age(false,Integer.class),;

        private final boolean key;
        private final Class<?> type;

        Property(boolean key, Class<?> type) {
            this.key = key;
            this.type = type;
        }

        @Override
        public boolean isKey() {
            return key;
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