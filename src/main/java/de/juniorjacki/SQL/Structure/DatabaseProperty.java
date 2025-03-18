package de.juniorjacki.SQL.Structure;

public interface DatabaseProperty {
    boolean isKey();
    boolean isUnique();
    Class<?> getType();
    String name();
}
