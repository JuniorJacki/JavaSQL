package de.juniorjacki.utils;



import de.juniorjacki.SQL.Structure.DatabaseProperty;
import de.juniorjacki.SQL.Structure.Table;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Record {
    public static <T> T populateRecord(Class<T> recordClass, String[] fieldNames, Object[] fieldValues) throws Exception {
        if (fieldNames.length != fieldValues.length) {
            throw new IllegalArgumentException("Field names and values must have the same length.");
        }
        Constructor<?> constructor = recordClass.getDeclaredConstructors()[0];
        Object[] constructorArgs = new Object[constructor.getParameterCount()];
        RecordComponent[] components = recordClass.getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            for (int j = 0; j < fieldNames.length; j++) {
                if (components[i].getName().equals(fieldNames[j])) {
                    constructorArgs[i] = fieldValues[j];
                    break;
                }
            }
        }
        return (T) constructor.newInstance(constructorArgs);
    }



    public static <T extends Table<E, ?>, E extends Enum<E> & DatabaseProperty> Object populateRecord(T table, ResultSet resultSet) throws Exception {
        Class<?> recordClass = table.getTableRecord();

        // Get the constructor of the record class
        Constructor<?> constructor = recordClass.getDeclaredConstructors()[0];
        Object[] constructorArgs = new Object[constructor.getParameterCount()];
        RecordComponent[] components = recordClass.getRecordComponents();

        // Get the properties from the table instance
        List<E> properties = table.getProperties();

        for (int i = 0; i < components.length; i++) {
            String componentName = components[i].getName();

            for (E property : properties) {
                if (componentName.equals(property.name())) {
                    try {
                        if (property.getType().equals(String.class)) {
                            constructorArgs[i] = resultSet.getString(componentName);
                        } else if (property.getType().equals(UUID.class)) {
                            constructorArgs[i] = UUID.fromString(resultSet.getString(componentName));
                        } else if (property.getType().equals(Integer.class)) {
                            constructorArgs[i] = resultSet.getInt(componentName);
                        } else if (property.getType().equals(JSONObject.class)) {
                            constructorArgs[i] = new JSONObject(resultSet.getString(componentName));
                        } else if (property.getType().equals(Long.class)) {
                            constructorArgs[i] = resultSet.getLong(componentName);
                        } else if (property.getType().equals(Double.class)) {
                            constructorArgs[i] = resultSet.getDouble(componentName);
                        } else if (property.getType().equals(Boolean.class)) {
                            constructorArgs[i] = resultSet.getBoolean(componentName);
                        } else {
                            constructorArgs[i] = resultSet.getObject(componentName);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Failed to read column: " + componentName);
                    }
                    break;
                }
            }
        }

        return constructor.newInstance(constructorArgs);
    }


    public static <T> T populateRecord(Class<T> recordClass, Map<String, Object> fieldMap) throws Exception {
        Constructor<?> constructor = recordClass.getDeclaredConstructors()[0];
        Object[] constructorArgs = new Object[constructor.getParameterCount()];
        RecordComponent[] components = recordClass.getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            constructorArgs[i] = fieldMap.get(component.getName());
        }
        return (T) constructor.newInstance(constructorArgs);
    }

    public static Object getValueByName(Object record, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = record.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(record);
    }

}

