package de.juniorjacki.SQL.Structure;


import de.juniorjacki.SQL.Type.DatabaseRecord;
import de.juniorjacki.SQL.Type.DatabaseType;

import java.util.ArrayList;
import java.util.List;

public abstract class Table<E extends Enum<E> & DatabaseProperty,R extends Record & DatabaseRecord<R,E>> {
    public String tableName() {
        return getTableRecord().getSimpleName();
    }
    public abstract Class<R> getTableRecord();
    public abstract List<E> getProperties();
    public void onCreation() throws Exception {}
    Table<?,?> getTableClass() {
        return Tables.valueOf(this.tableName()).instance;
    }

    public List<Table.Property> tableProperties() {
        List<Table.Property> properties = new ArrayList<>();
        for (DatabaseProperty property : getProperties()) {
            properties.add(new Table.Property(property.name(), property.isKey(), property.isUnique(), property.getType(), property.extendLength()));
        }
        return properties;
    }
    public record Property(String dbName, boolean key, boolean unique, DatabaseType dataType, int extendedLength) {};
}


