package de.juniorjacki.SQL.Structure;

public interface DatabaseProperty {
    boolean isKey();
    Class<?> getType();
    String name();
}
