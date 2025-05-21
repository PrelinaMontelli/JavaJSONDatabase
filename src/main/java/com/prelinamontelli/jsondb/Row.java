package com.prelinamontelli.jsondb;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class Row {
    // 使用 LinkedHashMap 保持列的插入顺序，这对于 SELECT * 等操作很重要
    private final Map<String, Object> data;

    public Row() {
        this.data = new LinkedHashMap<>();
    }

    @JsonCreator
    public Row(Map<String, Object> data) {
        this.data = new LinkedHashMap<>(data);
    }

    @JsonValue
    public Map<String, Object> getData() {
        return data;
    }

    public Object getValue(String columnName) {
        return data.get(columnName);
    }

    public void setValue(String columnName, Object value) {
        data.put(columnName, value);
    }

    /**
     * 根据列定义验证并设置值。
     * @param column 列定义
     * @param value 要设置的值
     * @throws IllegalArgumentException 如果值无效或列不存在
     */
    public void validateAndSetValue(Column column, Object value) {
        if (column == null) {
            throw new IllegalArgumentException("Column definition cannot be null.");
        }
        Object convertedValue = column.convertValue(value);
        if (convertedValue != null && !column.isValidValue(convertedValue)) {
            throw new IllegalArgumentException(
                "Invalid value '" + value + "' for column '" + column.getName() + "' of type " + column.getType()
            );
        }
        data.put(column.getName(), convertedValue);
    }

    /**
     * 更新行中给定列的现有值，执行验证和类型转换。
     * @param column 列定义。
     * @param newValue 要设置的新值。
     * @throws IllegalArgumentException 如果列在行中不存在（如果行来自表，则不应发生此情况），或者新值对于列类型无效。
     */
    public void updateValue(Column column, Object newValue) {
        if (column == null) {
            throw new IllegalArgumentException("Column definition cannot be null for update.");
        }
        Object convertedValue = column.convertValue(newValue);
        if (convertedValue != null && !column.isValidValue(convertedValue)) {
            throw new IllegalArgumentException(
                "Invalid new value '" + newValue + "' for column '" + column.getName() + "' of type " + column.getType()
            );
        }
        data.put(column.getName(), convertedValue);
    }

    public boolean hasColumn(String columnName) {
        return data.containsKey(columnName);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Row row = (Row) o;
        return Objects.equals(data, row.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    @Override
    public String toString() {
        return "Row{" +
               "data=" + data +
               '}';
    }
} 