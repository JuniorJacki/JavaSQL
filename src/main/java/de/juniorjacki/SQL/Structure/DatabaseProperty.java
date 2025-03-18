package de.juniorjacki.SQL.Structure;

public interface DatabaseProperty {
    /**
     * Defines If Column is a Primary Key
     */
    boolean isKey();

    /**
     * Defines if Column is Unique
     */
    boolean isUnique();
    /**
     * Defines if Column Datatype
     */
    Class<?> getType();
    /**
     * Defines if Column name
     */
    String name();
}
