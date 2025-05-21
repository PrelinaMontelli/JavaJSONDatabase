package com.prelinamontelli.jsondb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.Map;

// Imports for signature checking
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.security.CodeSigner;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
// Import for date formatting
import java.text.SimpleDateFormat;
import java.util.Date;

public class JsonDBCLI {
    private final DBManager dbManager;
    private final Scanner scanner;
    private final LocalizationService localizationService;

    public JsonDBCLI() {
        this.dbManager = new DBManager();
        this.scanner = new Scanner(System.in);
        this.localizationService = new LocalizationService();
    }

    public void start() {
        System.out.println(localizationService.getMessage("welcome_message"));
        displayPrompt();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.toUpperCase().endsWith(";")) {
                 String commandPart = line.substring(0, line.length() - 1).trim();
                 String[] tokens = commandPart.split("\\s+", 2);
                 String action = tokens[0].toUpperCase();

                if (action.equals("EXIT") || action.equals("QUIT")) {
                    break;
                }
                if (action.equals(localizationService.getMessage("command_set_language").toUpperCase())) {
                    if (tokens.length > 1) {
                        handleSetLanguage(tokens[1]);
                    } else {
                        System.err.println(localizationService.getMessage("error_invalid_syntax") + localizationService.getMessage("help_set_language"));
                    }
                    displayPrompt();
                    continue;
                }
            }
           
            if (line.isEmpty() || line.startsWith("--") || line.startsWith("//")) {
                displayPrompt();
                continue;
            }

            if (!line.endsWith(";")) {
                System.out.println(localizationService.getMessage("error_multiline_not_supported"));
                displayPrompt();
                continue;
            }

            String command = line.substring(0, line.length() - 1).trim();

            try {
                processCommand(command);
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.err.println(localizationService.getMessage("error_prefix") + e.getMessage());
            } catch (IOException e) {
                System.err.println(localizationService.getMessage("io_error_prefix") + e.getMessage());
            } catch (Exception e) {
                System.err.println(localizationService.getMessage("unexpected_error_prefix") + e.getMessage());
                e.printStackTrace(); 
            }
            displayPrompt();
        }
        shutdown();
    }

    private void displayPrompt() {
        String currentDbName = dbManager.getCurrentDatabaseName().orElse(localizationService.getMessage("db_prompt_none"));
        System.out.print(localizationService.getMessage("db_prompt_prefix") + " (" + currentDbName + ")> ");
    }

    private void processCommand(String command) throws IOException {
        String[] parts = command.split("\\s+", 2);
        String action = parts[0].toUpperCase();
        String arguments = parts.length > 1 ? parts[1] : "";

        switch (action) {
            case "CREATE":
                handleCreate(arguments);
                break;
            case "DROP":
                handleDrop(arguments);
                break;
            case "USE":
                handleUse(arguments);
                break;
            case "SHOW":
                handleShow(arguments);
                break;
            case "INSERT":
                handleInsert(arguments);
                break;
            case "SELECT":
                handleSelect(arguments);
                break;
            case "DELETE":
                handleDelete(arguments);
                break;
            case "UPDATE":
                handleUpdate(arguments);
                break;
            case "HELP":
                handleHelp();
                break;
            default:
                System.err.println(localizationService.getMessage("error_unknown_command", action));
        }
    }
    
    private void handleSetLanguage(String langCode) {
        if (localizationService.setLanguage(langCode)) {
            String langName = langCode;
            if (langCode.equalsIgnoreCase("en")) langName = "English";
            if (langCode.equalsIgnoreCase("zh")) langName = "中文 (Simplified Chinese)";
            System.out.println(localizationService.getMessage("message_language_set", langName));
        } else {
            System.err.println(localizationService.getMessage("error_unsupported_language", langCode, LocalizationService.getSupportedLanguageCodes().toString()));
        }
    }

    private void handleCreate(String arguments) throws IOException {
        String[] parts = arguments.split("\\s+", 2); 
        if (parts.length < 2) {
            System.err.println(localizationService.getMessage("usage_create_db_table"));
            return;
        }
        String type = parts[0].toUpperCase();
        String restOfArguments = parts[1];

        if ("DATABASE".equals(type)) {
            dbManager.createDatabase(restOfArguments);
        } else if ("TABLE".equals(type)) {
            int firstParen = restOfArguments.indexOf('(');
            int lastParen = restOfArguments.lastIndexOf(')');

            if (firstParen == -1 || lastParen == -1 || firstParen >= lastParen) {
                System.err.println(localizationService.getMessage("error_invalid_syntax") + "Usage: CREATE TABLE <tablename> (col1 type1, ...)");
                return;
            }

            String tableName = restOfArguments.substring(0, firstParen).trim();
            if (tableName.isEmpty()) {
                System.err.println(localizationService.getMessage("error_name_empty", "Table"));
                return;
            }

            String columnsString = restOfArguments.substring(firstParen + 1, lastParen).trim();
            if (columnsString.isEmpty()) {
                System.err.println(localizationService.getMessage("error_column_definitions_empty"));
                return;
            }

            String[] columnDefs = columnsString.split(",");
            List<Column> columnList = new ArrayList<>();

            for (String colDef : columnDefs) {
                String[] colParts = colDef.trim().split("\\s+");
                if (colParts.length != 2) {
                    System.err.println(localizationService.getMessage("error_invalid_column_definition", colDef.trim()));
                    return; 
                }
                String colName = colParts[0].trim();
                String colTypeStr = colParts[1].trim().toUpperCase();
                try {
                    DataType dataType = DataType.valueOf(colTypeStr);
                    columnList.add(new Column(colName, dataType));
                } catch (IllegalArgumentException e) {
                    System.err.println(localizationService.getMessage("error_invalid_data_type", colTypeStr, colName, Arrays.toString(DataType.values())));
                    return; 
                }
            }

            if (columnList.isEmpty()) {
                System.err.println(localizationService.getMessage("error_no_columns_defined", tableName));
                return;
            }

            Database currentDb = dbManager.ensureCurrentDatabaseSelected(); 
            currentDb.createTable(tableName, columnList);
            dbManager.saveCurrentDatabase(); 
            System.out.println(localizationService.getMessage("message_table_created", tableName, currentDb.getName()));

        } else {
            System.err.println(localizationService.getMessage("error_unknown_command", "CREATE " + type));
        }
    }

    private void handleDrop(String arguments) throws IOException {
        String[] parts = arguments.split("\\s+", 2);
        if (parts.length < 2) {
            System.err.println(localizationService.getMessage("usage_drop_db_table"));
            return;
        }
        String type = parts[0].toUpperCase();
        String name = parts[1];

        if ("DATABASE".equals(type)) {
            dbManager.dropDatabase(name);
        } else if ("TABLE".equals(type)) {
            System.out.println("DROP TABLE functionality not yet implemented.");
        } else {
            System.err.println(localizationService.getMessage("error_unknown_command", "DROP " + type));
        }
    }

    private void handleUse(String dbName) {
        if (dbName.isEmpty()) {
            System.err.println(localizationService.getMessage("usage_use_db"));
            return;
        }
        try {
            dbManager.useDatabase(dbName);
        } catch (IllegalArgumentException e){
             System.err.println(localizationService.getMessage("error_prefix") + e.getMessage());
        }
    }

    private void handleShow(String arguments) {
        String upperArgs = arguments.toUpperCase();
        if ("DATABASES".equals(upperArgs)) {
            List<String> dbNames = dbManager.listDatabaseNames();
            if (dbNames.isEmpty()) {
                System.out.println(localizationService.getMessage("message_no_databases_found"));
            } else {
                System.out.println(localizationService.getMessage("message_databases_list_header"));
                dbNames.forEach(System.out::println);
            }
        } else if ("TABLES".equals(upperArgs)) {
            try {
                Database currentDb = dbManager.ensureCurrentDatabaseSelected();
                List<String> tableNames = currentDb.listTableNames();
                if (tableNames.isEmpty()) {
                    System.out.println(localizationService.getMessage("message_no_tables_found", currentDb.getName()));
                } else {
                    System.out.println(localizationService.getMessage("message_tables_list_header", currentDb.getName()));
                    tableNames.forEach(System.out::println);
                }
            } catch (IllegalStateException e) {
                 System.err.println(localizationService.getMessage("error_prefix") + e.getMessage());
            }
        } else {
            System.err.println(localizationService.getMessage("error_unknown_command", "SHOW " + upperArgs));
        }
    }

    private void handleInsert(String arguments) throws IOException {
        String upperArguments = arguments.toUpperCase();
        if (!upperArguments.startsWith("INTO ")) {
            System.err.println(localizationService.getMessage("error_invalid_syntax") + "Usage: INSERT INTO <tablename> VALUES (...)");
            return;
        }
        String remainingArgs = arguments.substring("INTO ".length()).trim();
        int valuesKeywordPos = remainingArgs.toUpperCase().indexOf(" VALUES");
        if (valuesKeywordPos == -1) {
            System.err.println(localizationService.getMessage("error_missing_keyword", "VALUES"));
            return;
        }
        String tableName = remainingArgs.substring(0, valuesKeywordPos).trim();
        String valuesPart = remainingArgs.substring(valuesKeywordPos + " VALUES".length()).trim();

        if (tableName.isEmpty()) {
            System.err.println(localizationService.getMessage("error_name_empty", "Table (for INSERT)"));
            return;
        }
        if (!valuesPart.startsWith("(") || !valuesPart.endsWith(")")) {
            System.err.println(localizationService.getMessage("error_invalid_syntax") + "Values must be in parentheses.");
            return;
        }
        String valuesString = valuesPart.substring(1, valuesPart.length() - 1).trim();
        Database currentDbForCheck = dbManager.ensureCurrentDatabaseSelected();
        Table tableForCheck = currentDbForCheck.getTable(tableName)
            .orElseThrow(() -> new IllegalArgumentException(localizationService.getMessage("error_table_not_found", tableName, currentDbForCheck.getName())));

        if (valuesString.isEmpty() && !tableForCheck.getColumnDefinitions().isEmpty() ){
            System.err.println(localizationService.getMessage("error_values_list_empty"));
            return;
        }

        List<String> rawValueStrings = new ArrayList<>();
        if (!valuesString.isEmpty()) {
            List<String> tempList = new ArrayList<>();
            StringBuilder currentValue = new StringBuilder();
            boolean inQuotes = false;
            for (char c : valuesString.toCharArray()) {
                if (c == '"') {
                    inQuotes = !inQuotes;
                    currentValue.append(c); 
                } else if (c == ',' && !inQuotes) {
                    tempList.add(currentValue.toString().trim());
                    currentValue.setLength(0); 
                } else {
                    currentValue.append(c);
                }
            }
            tempList.add(currentValue.toString().trim()); 
            rawValueStrings.addAll(tempList);
        }
       
        Database currentDb = dbManager.ensureCurrentDatabaseSelected();
        Table table = currentDb.getTable(tableName)
                .orElseThrow(() -> new IllegalArgumentException(localizationService.getMessage("error_table_not_found", tableName, currentDb.getName())));

        List<Column> columns = table.getColumnDefinitions();
        if (rawValueStrings.size() != columns.size()) {
            System.err.println(localizationService.getMessage("error_column_count_mismatch_insert", tableName, columns.size(), rawValueStrings.size()));
            return;
        }

        Map<String, Object> rowData = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            String rawValue = rawValueStrings.get(i);
            Object actualValue;
            if (rawValue.equalsIgnoreCase("NULL")) {
                actualValue = null;
            } else if (rawValue.startsWith("\"") && rawValue.endsWith("\"") && rawValue.length() >= 2) {
                actualValue = rawValue.substring(1, rawValue.length() - 1); 
            } else {
                actualValue = rawValue; 
            }
            rowData.put(column.getName(), actualValue);
        }
        try {
            table.addRow(rowData); 
            dbManager.saveCurrentDatabase();
            System.out.println(localizationService.getMessage("message_row_inserted", tableName));
        } catch (IllegalArgumentException e) {
            System.err.println(localizationService.getMessage("error_prefix") + e.getMessage());
        }
    }

    private void handleSelect(String arguments) throws IOException {
        String upperArgs = arguments.toUpperCase();
        int fromPos = upperArgs.indexOf(" FROM ");
        if (fromPos == -1) {
            System.err.println(localizationService.getMessage("error_missing_keyword", "FROM (in SELECT)"));
            return;
        }
        String columnsPart = arguments.substring(0, fromPos).trim();
        String tableAndWherePart = arguments.substring(fromPos + " FROM ".length()).trim();
        String tableName; String whereClause = null;
        int whereKeywordPos = tableAndWherePart.toUpperCase().indexOf(" WHERE ");

        if (whereKeywordPos != -1) {
            tableName = tableAndWherePart.substring(0, whereKeywordPos).trim();
            whereClause = tableAndWherePart.substring(whereKeywordPos + " WHERE ".length()).trim();
            if (whereClause.isEmpty()) {
                System.err.println(localizationService.getMessage("error_empty_clause", "WHERE"));
                return;
            }
        } else {
            tableName = tableAndWherePart.trim();
        }
        if (tableName.isEmpty()) {
            System.err.println(localizationService.getMessage("error_name_empty", "Table (for SELECT)"));
            return;
        }
        if (columnsPart.isEmpty()) {
            System.err.println(localizationService.getMessage("error_invalid_syntax") + "Column selection cannot be empty.");
            return;
        }

        Database currentDb = dbManager.ensureCurrentDatabaseSelected();
        Table table = currentDb.getTable(tableName)
                .orElseThrow(() -> new IllegalArgumentException(localizationService.getMessage("error_table_not_found", tableName, currentDb.getName())));

        List<String> selectedColumnNames;
        List<Column> tableColumns = table.getColumnDefinitions();
        if (columnsPart.equals("*")) {
            selectedColumnNames = tableColumns.stream().map(Column::getName).collect(Collectors.toList());
        } else {
            List<String> rawSelectedColumnNames = Arrays.stream(columnsPart.split(",")).map(String::trim).collect(Collectors.toList());
            selectedColumnNames = new ArrayList<>();
            for (String rawScn : rawSelectedColumnNames) {
                boolean found = false;
                for (Column tableCol : tableColumns) {
                    if (tableCol.getName().equalsIgnoreCase(rawScn)) {
                        selectedColumnNames.add(tableCol.getName());
                        found = true; break;
                    }
                }
                if (!found) {
                    System.err.println(localizationService.getMessage("error_column_not_found", rawScn, tableName));
                    return;
                }
            }
        }
        if (selectedColumnNames.isEmpty()) {
            System.err.println("No columns selected or specified correctly.");
            return;
        }
        List<Row> resultRows = new ArrayList<>(table.getRows());
        if (whereClause != null) {
            String[] whereParts = whereClause.split("=", 2);
            if (whereParts.length != 2) {
                System.err.println(localizationService.getMessage("error_invalid_syntax") + "WHERE format: <col> = <val>");
                return;
            }
            String filterColumnName = whereParts[0].trim();
            String filterValueString = whereParts[1].trim();
            Column filterColumn = table.getColumn(filterColumnName)
                .orElseThrow(() -> new IllegalArgumentException(localizationService.getMessage("error_column_not_found_where", filterColumnName, tableName)));
            Object filterValue;
            if (filterValueString.equalsIgnoreCase("NULL")) {
                filterValue = null;
            } else if (filterValueString.startsWith("\"") && filterValueString.endsWith("\"") && filterValueString.length() >= 2) {
                filterValue = filterColumn.convertValue(filterValueString.substring(1, filterValueString.length() - 1));
            } else {
                filterValue = filterColumn.convertValue(filterValueString);
            }
            resultRows = resultRows.stream()
                                   .filter(row -> {
                                       Object rowValue = row.getValue(filterColumn.getName());
                                       return (filterValue == null) ? rowValue == null : filterValue.equals(rowValue);
                                   })
                                   .collect(Collectors.toList());
        }
        System.out.println(String.join("\t|\t", selectedColumnNames));
        selectedColumnNames.forEach(header -> {
            for (int i = 0; i < header.length() + 2; i++) System.out.print("-");
            System.out.print("-");
        });
        System.out.println();
        if (resultRows.isEmpty()) {
            System.out.println(localizationService.getMessage("message_select_0_rows"));
        } else {
            for (Row row : resultRows) {
                List<String> outputValues = new ArrayList<>();
                for (String colName : selectedColumnNames) {
                    Object val = row.getValue(colName);
                    outputValues.add(val == null ? "NULL" : val.toString());
                }
                System.out.println(String.join("\t|\t", outputValues));
            }
            String row_count_msg_key = resultRows.size() == 1 ? "message_row_singular" : "message_row_plural";
            System.out.println("(" + resultRows.size() + " " + localizationService.getMessage(row_count_msg_key) + ")");
        }
    }

    private void handleDelete(String arguments) throws IOException {
        String upperArgs = arguments.toUpperCase();
        if (!upperArgs.startsWith("FROM ")) {
            System.err.println(localizationService.getMessage("error_missing_keyword", "FROM (in DELETE)"));
            return;
        }
        String tableAndWherePart = arguments.substring("FROM ".length()).trim();
        String tableName; String whereClause = null;
        int whereKeywordPos = tableAndWherePart.toUpperCase().indexOf(" WHERE ");
        if (whereKeywordPos != -1) {
            tableName = tableAndWherePart.substring(0, whereKeywordPos).trim();
            whereClause = tableAndWherePart.substring(whereKeywordPos + " WHERE ".length()).trim();
            if (whereClause.isEmpty()) {
                System.err.println(localizationService.getMessage("error_empty_clause", "WHERE"));
                return;
            }
        } else {
            tableName = tableAndWherePart.trim();
        }
        if (tableName.isEmpty()) {
            System.err.println(localizationService.getMessage("error_name_empty", "Table (for DELETE)"));
            return;
        }
        Database currentDb = dbManager.ensureCurrentDatabaseSelected();
        Table table = currentDb.getTable(tableName)
                .orElseThrow(() -> new IllegalArgumentException(localizationService.getMessage("error_table_not_found", tableName, currentDb.getName())));
        
        List<Row> originalRows = new ArrayList<>(table.getRows());
        List<Row> rowsToDelete = new ArrayList<>();

        if (whereClause != null) {
            String[] whereParts = whereClause.split("=", 2);
            if (whereParts.length != 2) {
                System.err.println(localizationService.getMessage("error_invalid_syntax") + "WHERE format: <col> = <val>");
                return;
            }
            String filterColumnName = whereParts[0].trim();
            String filterValueString = whereParts[1].trim();
            Column filterColumn = table.getColumn(filterColumnName)
                .orElseThrow(() -> new IllegalArgumentException(localizationService.getMessage("error_column_not_found_where", filterColumnName, tableName)));
            Object filterValue;
            if (filterValueString.equalsIgnoreCase("NULL")) {
                filterValue = null;
            } else if (filterValueString.startsWith("\"") && filterValueString.endsWith("\"") && filterValueString.length() >= 2) {
                filterValue = filterColumn.convertValue(filterValueString.substring(1, filterValueString.length() - 1));
            } else {
                filterValue = filterColumn.convertValue(filterValueString);
            }
            for (Row row : originalRows) {
                Object rowValue = row.getValue(filterColumn.getName());
                if ((filterValue == null && rowValue == null) || (filterValue != null && filterValue.equals(rowValue))) {
                    rowsToDelete.add(row);
                }
            }
        } else {
            rowsToDelete.addAll(originalRows);
        }

        if (!rowsToDelete.isEmpty()) {
            boolean changed = table.getRows().removeAll(rowsToDelete);
            if (changed) {
                dbManager.saveCurrentDatabase();
                String singularPluralKey = rowsToDelete.size() == 1 ? "message_row_singular" : "message_row_plural";
                System.out.println(localizationService.getMessage("message_rows_deleted", rowsToDelete.size(), localizationService.getMessage(singularPluralKey)));
            } else {
                System.out.println(localizationService.getMessage("message_rows_affected_0")); 
            }
        } else {
            System.out.println(localizationService.getMessage("message_rows_affected_0"));
        }
    }

    private void handleUpdate(String arguments) throws IOException {
        String upperArgs = arguments.toUpperCase();
        int setPos = upperArgs.indexOf(" SET ");
        if (setPos == -1) {
            System.err.println(localizationService.getMessage("error_missing_keyword", "SET (in UPDATE)"));
            return;
        }
        String tableName = arguments.substring(0, setPos).trim();
        if (tableName.isEmpty()) {
            System.err.println(localizationService.getMessage("error_name_empty", "Table (for UPDATE)"));
            return;
        }
        String setAndWherePart = arguments.substring(setPos + " SET ".length()).trim();
        String setClauseString; String whereClause = null;
        int wherePos = setAndWherePart.toUpperCase().indexOf(" WHERE ");
        if (wherePos != -1) {
            setClauseString = setAndWherePart.substring(0, wherePos).trim();
            whereClause = setAndWherePart.substring(wherePos + " WHERE ".length()).trim();
            if (whereClause.isEmpty()) {
                System.err.println(localizationService.getMessage("error_empty_clause", "WHERE"));
                return;
            }
        } else {
            setClauseString = setAndWherePart.trim();
        }
        if (setClauseString.isEmpty()) {
            System.err.println(localizationService.getMessage("error_empty_clause", "SET"));
            return;
        }
        Map<String, String> setValues = new LinkedHashMap<>();
        if (!setClauseString.isEmpty()) {
            List<String> assignmentStrings = new ArrayList<>();
            StringBuilder currentAssignment = new StringBuilder();
            boolean inQuotes = false;
            for (char c : setClauseString.toCharArray()) {
                if (c == '"') {
                    inQuotes = !inQuotes;
                    currentAssignment.append(c);
                } else if (c == ',' && !inQuotes) {
                    assignmentStrings.add(currentAssignment.toString().trim());
                    currentAssignment.setLength(0);
                } else {
                    currentAssignment.append(c);
                }
            }
            assignmentStrings.add(currentAssignment.toString().trim());
            for (String assignment : assignmentStrings) {
                String[] parts = assignment.split("=", 2);
                if (parts.length != 2) {
                    System.err.println(localizationService.getMessage("error_invalid_syntax") + "SET format: <col>=<val>,...");
                    return;
                }
                setValues.put(parts[0].trim(), parts[1].trim());
            }
        }
        if (setValues.isEmpty()) {
            System.err.println(localizationService.getMessage("error_invalid_syntax") + "No assignments in SET.");
            return;
        }
        Database currentDb = dbManager.ensureCurrentDatabaseSelected();
        Table table = currentDb.getTable(tableName)
            .orElseThrow(() -> new IllegalArgumentException(localizationService.getMessage("error_table_not_found", tableName, currentDb.getName())));
        Map<Column, String> validatedSetOperations = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : setValues.entrySet()) {
            String colNameToSet = entry.getKey();
            Column tableColumn = table.getColumn(colNameToSet)
                .orElseThrow(() -> new IllegalArgumentException(localizationService.getMessage("error_column_not_found_set", colNameToSet, tableName)));
            validatedSetOperations.put(tableColumn, entry.getValue());
        }
        List<Row> rowsToUpdate = new ArrayList<>();
        if (whereClause != null) {
            String[] whereParts = whereClause.split("=", 2);
             if (whereParts.length != 2) {
                System.err.println(localizationService.getMessage("error_invalid_syntax") + "WHERE format: <col> = <val>");
                return;
            }
            String filterColumnName = whereParts[0].trim();
            String filterValueString = whereParts[1].trim();
            Column filterColumn = table.getColumn(filterColumnName)
                .orElseThrow(() -> new IllegalArgumentException(localizationService.getMessage("error_column_not_found_where", filterColumnName, tableName)));
            Object filterValue;
            if (filterValueString.equalsIgnoreCase("NULL")) {
                filterValue = null;
            } else if (filterValueString.startsWith("\"") && filterValueString.endsWith("\"") && filterValueString.length() >= 2) {
                filterValue = filterColumn.convertValue(filterValueString.substring(1, filterValueString.length() - 1));
            } else {
                filterValue = filterColumn.convertValue(filterValueString);
            }
            for (Row row : table.getRows()) {
                Object rowVal = row.getValue(filterColumn.getName());
                if ((filterValue == null && rowVal == null) || (filterValue != null && filterValue.equals(rowVal))) {
                    rowsToUpdate.add(row);
                }
            }
        } else {
            rowsToUpdate.addAll(table.getRows());
        }
        if (rowsToUpdate.isEmpty()) {
            System.out.println(localizationService.getMessage("message_rows_affected_0"));
            return;
        }
        int updatedCount = 0;
        try {
            for (Row row : rowsToUpdate) {
                boolean rowChanged = false;
                for (Map.Entry<Column, String> operation : validatedSetOperations.entrySet()) {
                    Column columnToUpdate = operation.getKey();
                    String rawNewValue = operation.getValue();
                    Object actualNewValue;
                    if (rawNewValue.equalsIgnoreCase("NULL")) {
                        actualNewValue = null;
                    } else if (rawNewValue.startsWith("\"") && rawNewValue.endsWith("\"") && rawNewValue.length() >=2) {
                        actualNewValue = rawNewValue.substring(1, rawNewValue.length() -1 );
                    } else {
                        actualNewValue = rawNewValue;
                    }
                    row.updateValue(columnToUpdate, actualNewValue);
                    rowChanged = true; 
                }
                if(rowChanged) updatedCount++;
            }
            if (updatedCount > 0) {
                 dbManager.saveCurrentDatabase();
            }
            String singularPluralKey = updatedCount == 1 ? "message_row_singular" : "message_row_plural";
            System.out.println(localizationService.getMessage("message_rows_updated", updatedCount, localizationService.getMessage(singularPluralKey)));
        } catch (IllegalArgumentException e) {
            System.err.println(localizationService.getMessage("error_prefix") + e.getMessage());
        }
    }

    private void handleHelp() {
        System.out.println(localizationService.getMessage("help_header"));
        System.out.println(localizationService.getMessage("help_separator"));
        System.out.println(localizationService.getMessage("help_db_management"));
        System.out.println(localizationService.getMessage("help_create_db"));
        System.out.println(localizationService.getMessage("help_drop_db"));
        System.out.println(localizationService.getMessage("help_use_db"));
        System.out.println(localizationService.getMessage("help_show_dbs"));
        System.out.println(localizationService.getMessage("help_table_management"));
        System.out.println(localizationService.getMessage("help_create_table"));
        System.out.println(localizationService.getMessage("help_supported_types"));
        System.out.println(localizationService.getMessage("help_show_tables"));
        System.out.println(localizationService.getMessage("help_data_manipulation"));
        System.out.println(localizationService.getMessage("help_insert_into"));
        System.out.println(localizationService.getMessage("help_insert_notes"));
        System.out.println(localizationService.getMessage("help_select"));
        System.out.println(localizationService.getMessage("help_select_notes"));
        System.out.println(localizationService.getMessage("help_update"));
        System.out.println(localizationService.getMessage("help_update_notes"));
        System.out.println(localizationService.getMessage("help_delete"));
        System.out.println(localizationService.getMessage("help_delete_notes"));
        System.out.println(localizationService.getMessage("help_utility"));
        System.out.println(localizationService.getMessage("help_help"));
        System.out.println(localizationService.getMessage("help_set_language"));
        System.out.println(localizationService.getMessage("help_exit"));
        System.out.println(localizationService.getMessage("help_separator"));
    }

    private void shutdown() {
        System.out.println(localizationService.getMessage("exit_message"));
        dbManager.shutdown();
        scanner.close();
        System.out.println(localizationService.getMessage("goodbye_message"));
    }

    // Helper method to convert byte array to hex string for thumbprint comparison
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static void main(String[] args) {
        // --- 代码自签检查 ---
        String expectedCertThumbprint = "503fafd5e12830436b49b597b289b8638db18d6d79fa302e631f7ca6f3b51394";  // 代码签名指纹，SHA-256，按需替换
        boolean signatureVerified = false;
        String signatureMessage = ""; // 将由本地化服务设置

        LocalizationService earlyLocalizationService = new LocalizationService(); // 用于启动时的消息
        String compilationTime = earlyLocalizationService.getMessage("compilation_time_unknown"); // 默认值

        try {
            URI codeSourceUri = JsonDBCLI.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File jarFile = new File(codeSourceUri);

            if (jarFile.isFile() && jarFile.getName().toLowerCase().endsWith(".jar")) {
                long lastModifiedTimestamp = jarFile.lastModified();
                if (lastModifiedTimestamp > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
                    compilationTime = sdf.format(new Date(lastModifiedTimestamp));
                }
                
                try (JarFile currentJar = new JarFile(jarFile)) {
                    // 我们需要验证至少一个已签名的条目。清单文件（MANIFEST.MF）通常是已签名的。
                    JarEntry manifestEntry = currentJar.getJarEntry("META-INF/MANIFEST.MF");
                    if (manifestEntry == null) {
                        // 后备方案：如果找不到清单文件（对于典型的已签名JAR不应发生此情况），尝试验证类文件。
                        String classFileEntryName = JsonDBCLI.class.getName().replace('.', '/') + ".class";
                        manifestEntry = currentJar.getJarEntry(classFileEntryName);
                    }

                    if (manifestEntry != null) {
                        // 读取条目内容是必要的，以使 JarFile 加载其签名者信息。
                        try (InputStream is = currentJar.getInputStream(manifestEntry)) {
                            byte[] buffer = new byte[8192];
                            while (is.read(buffer, 0, buffer.length) != -1) { /* 仅读取以触发签名者加载 */ }
                        } // InputStream 在此关闭
                        CodeSigner[] signers = manifestEntry.getCodeSigners();
                        if (signers != null && signers.length > 0) {
                            // 假设第一个签名者是我们感兴趣的。
                            // 在具有多个签名者的更复杂场景中，您可能需要迭代或识别正确的签名者。
                            X509Certificate signingCert = (X509Certificate) signers[0].getSignerCertPath().getCertificates().get(0);
                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                            byte[] digest = md.digest(signingCert.getEncoded());
                            String actualCertThumbprint = bytesToHex(digest);
                            if (expectedCertThumbprint.equalsIgnoreCase(actualCertThumbprint)) {
                                signatureVerified = true;
                            }
                        }
                    }
                } // JarFile 在此关闭
            } else {
                // 从IDE或独立的类文件运行（非JAR包），跳过签名检查。
                System.out.println(earlyLocalizationService.getMessage("signature_check_skipped_ide"));
                // 在这种情况下，如果您不希望出现警告，可以将 signatureVerified 视为 true 或不适用。
                // 在本示例中，我们将其保留为 false，因此如果不是JAR包，它会显示"未验证"消息。
                // 如果您希望将IDE执行视为"受信任的"，请在此处设置 signatureVerified = true。
            }
        } catch (Exception e) {
            // 保持 signatureVerified 为 false，compilationTime 可能为 "unknown"
            // System.err.println("签名检查期间发生错误: " + e.getMessage()); // 用于调试
        }

        if (signatureVerified) {
            signatureMessage = earlyLocalizationService.getMessage("signature_check_passed", compilationTime);
        } else {
            // 检查指纹是否仍为原始占位符，这表示配置错误
            if ("YOUR_CERTIFICATES_SHA256_THUMBPRINT_HERE".equalsIgnoreCase(expectedCertThumbprint)) { 
                 signatureMessage = earlyLocalizationService.getMessage("signature_check_misconfigured");
            } else {
                 signatureMessage = earlyLocalizationService.getMessage("signature_check_failed", compilationTime);
            }
        }
        System.out.println(signatureMessage);
        // --- X.509 自签名检查结束 ---

        JsonDBCLI cli = new JsonDBCLI();
        cli.start();
    }
} 