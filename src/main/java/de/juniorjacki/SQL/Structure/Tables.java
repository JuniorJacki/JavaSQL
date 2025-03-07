package de.juniorjacki.SQL.Structure;

import de.juniorjacki.SQL.Structure.DataTable.ExampleTable;

public enum Tables {
    Example(ExampleTable.Instance),;

    public final Table<?,?> instance;
    Tables(Table<?,?> instance) {
        this.instance = instance;
    }
}


