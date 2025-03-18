package de.juniorjacki.SQL.Structure;


import java.util.ArrayList;
import java.util.List;

public abstract class Table<E extends Enum<E> & DatabaseProperty,R extends Record & DatabaseRecord<R,E>> {
    public String tableName() {
        return getTableRecord().getSimpleName();
    }

    public abstract Class<R> getTableRecord();
    public abstract List<E> getProperties();
    public void onCreation() throws Exception {}

    public List<Property> tableProperties() {
        List<Property> properties = new ArrayList<>();
        for (DatabaseProperty property : getProperties()) {
            properties.add(new Property(property.name(), property.isKey(), property.isUnique(), property.getType()));
        }
        return properties;
    }
    public record Property(String dbName,boolean key,boolean unique,Class<?> dataType) {};
}


