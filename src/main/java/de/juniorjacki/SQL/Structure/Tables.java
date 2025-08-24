package de.juniorjacki.SQL.Structure;

import de.juniorjacki.SQL.Structure.DataTable.ExampleTable;
import de.juniorjacki.SQL.Structure.DataTable.LicenseTable;

public enum Tables {
    Example(ExampleTable.Instance),
    License(LicenseTable.Instance),;
    public final Table<?,?> instance;
    Tables(Table<?,?> instance) {
        this.instance = instance;
    }
}


