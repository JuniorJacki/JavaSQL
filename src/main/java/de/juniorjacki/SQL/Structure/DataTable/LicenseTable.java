package de.juniorjacki.SQL.Structure.DataTable;

import de.juniorjacki.SQL.Interface.DatabaseInterface;
import de.juniorjacki.SQL.Interface.QueryBuilder;
import de.juniorjacki.SQL.Structure.DatabaseProperty;
import de.juniorjacki.SQL.Structure.Table;
import de.juniorjacki.SQL.Type.DatabaseRecord;
import de.juniorjacki.SQL.Type.DatabaseType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LicenseTable extends Table<LicenseTable.Property,LicenseTable.License> implements DatabaseInterface<LicenseTable,LicenseTable.License,LicenseTable.Property>, QueryBuilder<LicenseTable,LicenseTable.License,LicenseTable.Property> {
    @Override
    public Class<LicenseTable.License> getTableRecord() {
        return LicenseTable.License.class;
    }

    @Override
    public List<LicenseTable.Property> getProperties() {
        return Arrays.asList(LicenseTable.Property.values());
    }

    @Override
    public LicenseTable getInstance() {
        return this;
    }

    public record License(UUID uID,String value,Long creationTimestamp) implements DatabaseRecord<LicenseTable.License, LicenseTable.Property> {
        @Override
        public LicenseTable.License getInstance() {
            return this;
        }
    }

    public enum Property implements DatabaseProperty {

        uID(true, DatabaseType.UUID), // Defines a Column named uID that is a Primary Key with the Datatype UUID -> VARCHAR(36)
        value(false, DatabaseType.STRING), // Defines a Column named preName with the Datatype String -> VARCHAR(255)
        creationTimestamp(false,DatabaseType.LONG),; // Defines a Column named creationTimestamp Long -> BIGINT

        private final boolean key;
        private final boolean unique;
        private final DatabaseType type;


        Property(boolean key, DatabaseType type) {
            this.key = key;
            unique = false;
            this.type = type;
        }

        Property(boolean key,boolean unique, DatabaseType type) {
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
        public DatabaseType getType() {
            return type;
        }

        @Override
        public int extendLength() {
            return 0;
        }
    }
    public static LicenseTable Instance = new LicenseTable();


    // Custom Method
    public Optional<LicenseTable.License> getLatestRecord() {
        return getByOrder(LicenseTable.Property.uID,Order.DESCENDING);
    }
}