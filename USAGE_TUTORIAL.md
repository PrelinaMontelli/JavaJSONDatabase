# PrelinaJsonDB 使用教程

本教程将引导您完成 Prelina JsonDB 的基本使用方法，包括数据库和表的创建、数据的增删改查等操作。

## 1. 启动程序

确保您已经按照 `README.md` 中的说明成功编译或获得了可执行的 JAR 文件 (例如 `json-db-1.0-SNAPSHOT.jar`)，并且您的系统中已安装 Java 8 或更高版本。

打开您的终端或命令提示符，导航到包含 JAR 文件的目录，然后运行：

```bash
java -jar json-db-1.0-SNAPSHOT.jar
```

成功启动后，您会看到欢迎信息和命令提示符 `jsondb (无)>`，表示当前没有选择任何数据库。

## 2. 基本命令格式

*   所有命令都必须以分号 (`;`) 结尾。
*   命令不区分大小写，但本教程中的关键字将使用大写以便识别。
*   字符串值通常需要用双引号 (`"`) 包围，例如 `"some string value"`。
*   使用 `NULL` (不带引号) 来表示空值。

## 3. 数据库操作

### 3.1 创建数据库

使用 `CREATE DATABASE` 命令创建一个新的数据库。数据库名称不应包含空格。

**示例：**
```sql
CREATE DATABASE my_first_db;
```
成功后会提示数据库已创建。

### 3.2 查看所有数据库

使用 `SHOW DATABASES` 命令列出所有已创建的数据库。

**示例：**
```sql
SHOW DATABASES;
```

### 3.3 切换当前数据库

在进行表操作和数据操作之前，您需要使用 `USE` 命令选择一个要操作的数据库。命令提示符会显示当前正在使用的数据库名称。

**示例：**
```sql
USE my_first_db;
```
成功后，命令提示符会变为 `jsondb (my_first_db)>`。

### 3.4 删除数据库

使用 `DROP DATABASE` 命令删除一个数据库及其包含的所有表和数据。此操作无法撤销，请谨慎使用。

**示例：**
```sql
DROP DATABASE my_first_db;
```

## 4. 表操作

确保您已经使用 `USE` 命令选择了一个数据库。

### 4.1 创建表

使用 `CREATE TABLE` 命令在当前数据库中创建一个新表。您需要定义表的名称以及列名和对应的数据类型。

支持的数据类型包括：
*   `INTEGER`：整数
*   `DOUBLE`：浮点数
*   `STRING`：字符串
*   `BOOLEAN`：布尔值 (`true` 或 `false`)

**示例：** 创建一个名为 `users` 的表，包含 `id` (整数)，`name` (字符串)，和 `email` (字符串) 列。
```sql
CREATE TABLE users (
    id INTEGER,
    name STRING,
    email STRING
);
```

### 4.2 查看当前数据库中的表

使用 `SHOW TABLES` 命令列出当前选定数据库中的所有表。

**示例：**
```sql
SHOW TABLES;
```

## 5. 数据操作

确保已选择数据库，并且相关的表已创建。

### 5.1 插入数据

使用 `INSERT INTO` 命令向指定的表中插入一行新数据。值的顺序和数量必须与表定义中的列匹配。

**示例：** 向 `users` 表插入一条数据。
```sql
INSERT INTO users VALUES (1, "Alice Wonderland", "alice@example.com");
INSERT INTO users VALUES (2, "Bob The Builder", "bob@example.com");
INSERT INTO users VALUES (3, "Charlie Brown", NULL); -- email 为空
```

### 5.2 查询数据

使用 `SELECT` 命令从表中检索数据。

#### 5.2.1 查询所有列

使用星号 (`*`) 查询表中的所有列。

**示例：**
```sql
SELECT * FROM users;
```

#### 5.2.2 查询特定列

指定列名来查询特定的列。

**示例：** 查询 `users` 表的 `name` 和 `email` 列。
```sql
SELECT name, email FROM users;
```

#### 5.2.3 带条件的查询 (WHERE)

使用 `WHERE` 子句根据条件筛选数据。目前支持简单的等值比较 (`=`)(`=`).

**示例：** 查询 `id` 为 `1` 的用户。
```sql
SELECT * FROM users WHERE id = 1;
```

**示例：** 查询 `email` 为 `NULL` 的用户。
```sql
SELECT * FROM users WHERE email = NULL;
```

**示例：** 查询 `name` 为 `"Bob The Builder"` 的用户。
```sql
SELECT * FROM users WHERE name = "Bob The Builder";
```

### 5.3 更新数据

使用 `UPDATE` 命令修改表中的现有数据。可以同时更新一个或多个列。

**示例：** 更新 `id` 为 `3` 的用户的 `email`。
```sql
UPDATE users SET email = "charlie@example.com" WHERE id = 3;
```

**示例：** 更新 `name` 为 `"Alice Wonderland"` 的用户的 `name` 和 `email` (假设我们想更改她的姓氏)。
```sql
UPDATE users SET name = "Alice Kingsleigh", email = "alice.k@example.com" WHERE name = "Alice Wonderland";
```

如果不指定 `WHERE` 子句，将更新表中的所有行（请谨慎操作！）。

### 5.4 删除数据

使用 `DELETE FROM` 命令从表中删除数据。

**示例：** 删除 `id` 为 `2` 的用户。
```sql
DELETE FROM users WHERE id = 2;
```

如果不指定 `WHERE` 子句，将删除表中的所有行（请谨慎操作！）。

**示例：** 删除 `users` 表中的所有数据。
```sql
DELETE FROM users;
```

## 6. 切换语言

使用 `SET LANGUAGE` 命令切换命令行界面的显示语言。目前支持 `en` (英文) 和 `zh` (简体中文)。

**示例：**
```sql
SET LANGUAGE en;
```
切换到英文。

```sql
SET LANGUAGE zh;
```
切换到中文。

## 7. 获取帮助

如果您忘记了命令的语法，可以使用 `HELP` 命令查看所有支持的命令及其基本用法。

**示例：**
```sql
HELP;
```

## 8. 退出程序

使用 `EXIT;` 或 `QUIT;` 命令关闭 PrelinaJsonDB。程序在退出时会自动保存所有未保存的更改。

**示例：**
```sql
QUIT;
```

---

