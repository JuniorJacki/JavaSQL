package de.juniorjacki.SQL.Base;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.juniorjacki.SQL.Type.Record.populateRecord;


public class SQLInputFilter {
    private static final List<String> blackList = List.of(" select", " insert", " update", " delete", " drop", " alter", " create", " exec", " union", " join");

    // Filters Bad Keywords
    public static <T> T filterExternalInput(T input) throws Exception {
        return switch (input) {
            case String text -> (T) filterString(text);
            case Record record -> (T) filterRecord(record);
            case List<?> list -> (T) filterList(list);
            default -> input;
        };
    }

    private static String filterString(String input) {
        String lowerText = input.toLowerCase();
        boolean isModified = false;
        StringBuilder filteredText = new StringBuilder(input);
        for (String blackWord : blackList) {
            int index;
            while ((index = lowerText.indexOf(blackWord)) != -1) {
                filteredText.replace(index, index + blackWord.length(), "");
                lowerText = filteredText.toString().toLowerCase();
                isModified = true;
            }
        }
        return isModified ? filteredText.toString() : input;
    }

    private static <R extends Record> R filterRecord(R record) throws Exception {
        Class<?> recordClass = record.getClass();
        Map<String, Object> fieldMap = new HashMap<>();
        boolean isModified = false;
        for (Field field : recordClass.getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(record);
            if (value instanceof String) {
                String filteredValue = filterString((String) value);
                if (!filteredValue.equals(value)) {
                    isModified = true;
                }
                fieldMap.put(field.getName(), filteredValue);
            } else {
                fieldMap.put(field.getName(), value);
            }
        }
        if (isModified) {
            return (R) populateRecord(recordClass, fieldMap);
        } else {
            return record;
        }
    }

    private static <T> List<T> filterList(List<T> list) {
        return list.stream()
                .map(item -> {
                    try {
                        return filterExternalInput(item);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }
}
