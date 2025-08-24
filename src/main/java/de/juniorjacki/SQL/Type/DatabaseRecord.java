package de.juniorjacki.SQL.Type;

import de.juniorjacki.SQL.Structure.DatabaseProperty;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public interface DatabaseRecord<T, F extends Enum<F> & DatabaseProperty> {

    T getInstance();

    /**
     * Returns updated Record
     * @param field    Field of Datatable
     * @param newValue Needs to be correct Datatype
     * @return New Record
     */
    default T editField(F field, Object newValue) throws Exception {
        if (!field.getType().getTypeClass().isInstance(newValue)) {
            throw new IllegalArgumentException("Invalid type for field " + field.name());
        }

        Class<?> recordClass = getInstance().getClass();
        Field[] fields = recordClass.getDeclaredFields();
        Object[] fieldValues = new Object[fields.length];

        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            fieldValues[i] = fields[i].get(getInstance());
        }

        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getName().equalsIgnoreCase(field.name())) {
                fieldValues[i] = newValue;
                break;
            }
        }

        Class<?>[] parameterTypes = new Class[fields.length];
        for (int i = 0; i < fields.length; i++) {
            parameterTypes[i] = fields[i].getType();
        }
        Constructor<?> constructor = recordClass.getDeclaredConstructor(parameterTypes);
        return (T) constructor.newInstance(fieldValues);

    }


}
