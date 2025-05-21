package com.prelinamontelli.jsondb;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Database {
    private final String name;
    private final Map<String, Table> tables; // 使用 ConcurrentHashMap 保证线程安全

    @JsonCreator
    public Database(@JsonProperty("name") String name, @JsonProperty("tables") Map<String, Table> tables) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Database name cannot be null or empty.");
        }
        this.name = name.trim();
        this.tables = tables == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(tables);
    }

    public Database(String name) {
        this(name, new ConcurrentHashMap<>());
    }

    public String getName() {
        return name;
    }

    public Map<String, Table> getTables() {
        return tables; // 返回的是 ConcurrentHashMap，直接操作是线程安全的
    }

    public void createTable(String tableName, List<Column> columns) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty for creation.");
        }
        String trimmedTableName = tableName.trim();
        if (tables.containsKey(trimmedTableName)) {
            throw new IllegalStateException("Table '" + trimmedTableName + "' already exists in database '" + this.name + "'.");
        }
        Table newTable = new Table(trimmedTableName, columns);
        tables.put(trimmedTableName, newTable);
    }

    public Optional<Table> getTable(String tableName) {
        if (tableName == null) return Optional.empty();
        return Optional.ofNullable(tables.get(tableName.trim()));
    }

    public boolean dropTable(String tableName) {
        if (tableName == null) return false;
        return tables.remove(tableName.trim()) != null;
    }

    public List<String> listTableNames() {
        return tables.keySet().stream().collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Database database = (Database) o;
        return name.equals(database.name) &&
               Objects.equals(tables, database.tables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tables);
    }

    @Override
    public String toString() {
        return "Database{" +
               "name='" + name + '\'' +
               ", tables_count=" + tables.size() +
               '}';
    }
} 