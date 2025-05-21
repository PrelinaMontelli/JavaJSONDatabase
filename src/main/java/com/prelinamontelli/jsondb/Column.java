package com.prelinamontelli.jsondb;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Column {
    private final String name;
    private final DataType type;

    @JsonCreator
    public Column(@JsonProperty("name") String name, @JsonProperty("type") DataType type) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be null or empty.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Column type cannot be null.");
        }
        this.name = name.trim();
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }

    // 校验值是否符合列的数据类型
    public boolean isValidValue(Object value) {
        if (value == null) {
            return true; // 允许 NULL 值
        }
        switch (type) {
            case INTEGER:
                return value instanceof Integer;
            case DOUBLE:
                return value instanceof Double || value instanceof Float || value instanceof Integer; // 允许 Integer 自动转换为 Double
            case STRING:
                return value instanceof String;
            case BOOLEAN:
                return value instanceof Boolean;
            default:
                return false;
        }
    }
    
    // 将值转换为此列的类型
    public Object convertValue(Object value) {
        if (value == null) {
            return null;
        }
        switch (type) {
            case INTEGER:
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                try {
                    return Integer.parseInt(String.valueOf(value));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Cannot convert '" + value + "' to INTEGER for column '" + name + "'");
                }
            case DOUBLE:
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
                try {
                    return Double.parseDouble(String.valueOf(value));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Cannot convert '" + value + "' to DOUBLE for column '" + name + "'");
                }
            case STRING:
                return String.valueOf(value);
            case BOOLEAN:
                if (value instanceof Boolean) {
                    return value;
                }
                String sValue = String.valueOf(value).toLowerCase();
                if ("true".equals(sValue)) {
                    return true;
                } else if ("false".equals(sValue)) {
                    return false;
                }
                throw new IllegalArgumentException("Cannot convert '" + value + "' to BOOLEAN for column '" + name + "'");

            default:
                throw new IllegalStateException("Unsupported data type: " + type);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Column column = (Column) o;
        return name.equals(column.name) && type == column.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return "Column{" +
               "name='" + name + '\'' +
               ", type=" + type +
               '}';
    }
} 