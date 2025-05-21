package com.prelinamontelli.jsondb;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Table {
    private final String name;
    // 使用 LinkedHashMap 保持列的定义顺序，并允许通过名称快速查找列
    private final Map<String, Column> columns;
    private final List<Row> rows;

    @JsonCreator
    public Table(@JsonProperty("name") String name, 
                 @JsonProperty("columns") List<Column> columnList, 
                 @JsonProperty("rows") List<Row> rows) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty.");
        }
        if (columnList == null || columnList.isEmpty()) {
            throw new IllegalArgumentException("Table must have at least one column.");
        }
        this.name = name.trim();
        this.columns = new LinkedHashMap<>();
        for (Column col : columnList) {
            if (this.columns.containsKey(col.getName())) {
                throw new IllegalArgumentException("Duplicate column name: " + col.getName() + " in table " + this.name);
            }
            this.columns.put(col.getName(), col);
        }
        this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
    }

    public Table(String name, List<Column> columnList) {
        this(name, columnList, new ArrayList<>());
    }

    public String getName() {
        return name;
    }

    @JsonProperty("columns") // 确保在序列化时使用列表而不是Map
    public List<Column> getColumnDefinitions() {
        return new ArrayList<>(columns.values());
    }

    public Optional<Column> getColumn(String columnName) {
        return Optional.ofNullable(columns.get(columnName));
    }

    public List<Row> getRows() {
        return rows; // 返回副本以防止外部修改？目前直接返回，后续可考虑
    }

    public void addRow(Row row) {
        if (row == null) {
            throw new IllegalArgumentException("Row cannot be null.");
        }
        // 验证 row 是否包含所有必需的列，并且值的类型是否正确
        // 确保 row 中的列名与表定义的列名完全匹配
        if (row.getData().size() != columns.size()) {
             throw new IllegalArgumentException(
                String.format("Row column count (%d) does not match table column count (%d) for table '%s'. Provided columns: %s, Expected columns: %s", 
                row.getData().size(), columns.size(), name, row.getData().keySet(), columns.keySet()));
        }

        Row validatedRow = new Row();
        for (Column tableColumn : columns.values()) {
            String colName = tableColumn.getName();
            if (!row.hasColumn(colName)) {
                throw new IllegalArgumentException(
                    String.format("Missing column '%s' in a row for table '%s'.", colName, name)
                );
            }
            Object value = row.getValue(colName);
            // 使用 Row 内部的 validateAndSetValue，它会调用 Column 的 convertValue 和 isValidValue
            validatedRow.validateAndSetValue(tableColumn, value);
        }
        rows.add(validatedRow); // 添加经过验证和转换的行
    }
    
    // 方便地通过 Map 创建并添加行
    public void addRow(Map<String, Object> rowData) {
        if (rowData == null) {
            throw new IllegalArgumentException("Row data cannot be null.");
        }
        Row newRow = new Row();
        for (Column tableColumn : columns.values()) {
            String colName = tableColumn.getName();
            Object value = rowData.get(colName); // 允许 null 值，validateAndSetValue 会处理
            // 如果列在 rowData 中不存在，则 value 将为 null
            // validateAndSetValue 内部会调用 column.convertValue 和 column.isValidValue
            newRow.validateAndSetValue(tableColumn, value);
        }
        rows.add(newRow);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Table table = (Table) o;
        return name.equals(table.name) &&
               Objects.equals(columns, table.columns) &&
               Objects.equals(rows, table.rows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columns, rows);
    }

    @Override
    public String toString() {
        return "Table{" +
               "name='" + name + '\'' +
               ", columns=" + columns.values() +
               ", rows_count=" + rows.size() +
               '}';
    }
} 