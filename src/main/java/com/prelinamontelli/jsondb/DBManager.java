package com.prelinamontelli.jsondb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DBManager {
    private static final String DEFAULT_DATA_DIR = "data";
    private final Path dataDirectory;
    private final ObjectMapper objectMapper;
    private final Map<String, Database> databases; // 数据库名称 -> 数据库对象
    private String currentDatabaseName; // 当前选择的数据库名

    public DBManager() {
        this(Paths.get(DEFAULT_DATA_DIR));
    }

    public DBManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.databases = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT); // Pretty print JSON
        objectMapper.registerModule(new JavaTimeModule()); // 支持 Java 8 Date/Time API (如果以后用到)
        // objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY); // 如果需要序列化私有字段

        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            loadDatabases();
        } catch (IOException e) {
            // 通常在 CLI 中，我们会打印错误并可能退出
            // 此处暂时作为运行时异常抛出，以便早期发现问题
            throw new RuntimeException("Failed to initialize data directory or load databases: " + dataDirectory, e);
        }
    }

    private void loadDatabases() throws IOException {
        File[] dbFiles = dataDirectory.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (dbFiles != null) {
            for (File dbFile : dbFiles) {
                try {
                    Database db = objectMapper.readValue(dbFile, Database.class);
                    databases.put(db.getName(), db);
                    System.out.println("Loaded database: " + db.getName());
                } catch (IOException e) {
                    System.err.println("Failed to load database from file " + dbFile.getName() + ": " + e.getMessage());
                    // 可以选择跳过损坏的文件，或者抛出异常停止服务
                }
            }
        }
    }

    private Path getDatabasePath(String dbName) {
        return dataDirectory.resolve(dbName + ".json");
    }

    public synchronized void createDatabase(String dbName) throws IOException {
        if (dbName == null || dbName.trim().isEmpty()) {
            throw new IllegalArgumentException("Database name cannot be null or empty.");
        }
        String trimmedDbName = dbName.trim();
        if (databases.containsKey(trimmedDbName)) {
            throw new IllegalStateException("Database '" + trimmedDbName + "' already exists.");
        }
        Database newDb = new Database(trimmedDbName);
        databases.put(trimmedDbName, newDb);
        saveDatabase(newDb); // 保存到文件
        System.out.println("Database '" + trimmedDbName + "' created.");
    }

    public synchronized void saveDatabase(Database db) throws IOException {
        if (db == null) {
            throw new IllegalArgumentException("Cannot save a null database.");
        }
        Path dbPath = getDatabasePath(db.getName());
        objectMapper.writeValue(dbPath.toFile(), db);
    }
    
    // 保存当前数据库 (如果已选择)
    public synchronized void saveCurrentDatabase() throws IOException {
        Database currentDb = getCurrentDatabase().orElse(null);
        if (currentDb != null) {
            saveDatabase(currentDb);
        }
    }

    public Optional<Database> getDatabase(String dbName) {
        if (dbName == null) return Optional.empty();
        return Optional.ofNullable(databases.get(dbName.trim()));
    }

    public synchronized void dropDatabase(String dbName) throws IOException {
        if (dbName == null || dbName.trim().isEmpty()) {
            throw new IllegalArgumentException("Database name cannot be null or empty for drop operation.");
        }
        String trimmedDbName = dbName.trim();
        Database removedDb = databases.remove(trimmedDbName);
        if (removedDb == null) {
            throw new IllegalStateException("Database '" + trimmedDbName + "' not found.");
        }
        Path dbPath = getDatabasePath(trimmedDbName);
        Files.deleteIfExists(dbPath);
        if (currentDatabaseName != null && currentDatabaseName.equals(trimmedDbName)) {
            currentDatabaseName = null; // 如果删除的是当前数据库，则取消选择
        }
        System.out.println("Database '" + trimmedDbName + "' dropped.");
    }

    public List<String> listDatabaseNames() {
        return databases.keySet().stream().sorted().collect(Collectors.toList());
    }

    public void useDatabase(String dbName) {
        if (dbName == null || !databases.containsKey(dbName.trim())) {
            throw new IllegalArgumentException("Database '" + dbName + "' not found. Cannot use.");
        }
        this.currentDatabaseName = dbName.trim();
        System.out.println("Now using database '" + this.currentDatabaseName + "'.");
    }

    public Optional<Database> getCurrentDatabase() {
        if (currentDatabaseName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(databases.get(currentDatabaseName));
    }

    public Optional<String> getCurrentDatabaseName() {
        return Optional.ofNullable(currentDatabaseName);
    }
    
    // Helper method for CLI/Engine to get the current DB or throw
    public Database ensureCurrentDatabaseSelected() {
        return getCurrentDatabase().orElseThrow(() -> 
            new IllegalStateException("No database selected. Use 'USE <database_name>;' first.")
        );
    }

    // 可以在应用关闭时调用，确保所有更改都已保存
    public synchronized void shutdown() {
        System.out.println("Shutting down database manager...");
        databases.values().forEach(db -> {
            try {
                saveDatabase(db);
                System.out.println("Saved database: " + db.getName());
            } catch (IOException e) {
                System.err.println("Failed to save database " + db.getName() + " on shutdown: " + e.getMessage());
            }
        });
        System.out.println("Database manager shutdown complete.");
    }
} 