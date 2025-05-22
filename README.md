# PrelinaJsonDB - 命令行JSON数据库

## 简介 (Overview)

Prelina JsonDB 是一个基于 Java 实现的轻量级命令行 JSON 数据库，为小组期末作业的一部分。它支持：

*   **基本的增删改查操作**：创建 (Create)、读取 (Read)、更新 (Update)、删除 (Delete) 数据。
*   **文件持久化**：数据库和表结构及数据会以 JSON 格式保存到本地文件中。
*   **多数据库和多表支持**：允许创建和管理多个独立的数据库，每个数据库可以包含多个表。
*   **命令行界面 (CLI)**：通过类似 SQL 的命令与数据库进行交互。
*   **国际化 (i18n)**：支持多语言界面（当前支持英文和简体中文）。

## 安装与运行预编译版本 (Installation and Running Pre-compiled Version)

### 先决条件 (Prerequisites)

*   **Java 运行环境 (JRE)**：版本 8 或更高。

### 运行步骤 (Steps)

本包已包含了预编译的 JAR 文件 ( `json-db-1.0-SNAPSHOT.jar`)，您可以按照以下步骤运行：

1.  确保您的系统中已安装 Java。您可以在终端或命令提示符中输入 `java -version` 来检查。
2.  将 target 文件夹下的`json-db-1.0-SNAPSHOT.jar` 文件放置在您希望的任意目录下。
3.  打开终端或命令提示符，导航到包含 JAR 文件的目录。
4.  执行以下命令来启动应用程序：

    ```bash
    java -jar json-db-1.0-SNAPSHOT.jar
    ```
    (请根据实际的 JAR 文件名进行调整)

## 如何自行编译 (How to Compile It Yourself)


推荐您使用macOS，因为在macOS上配置会很简单。


如果您希望从源代码自行编译项目，请按以下步骤操作：

### 先决条件 (Prerequisites)

*   **Java 开发工具包 (JDK)**：版本 8 或更高。
*   **Apache Maven**: 用于项目构建和依赖管理。

### 编译步骤 (Steps)

1.  **获取源代码**：
    确保您拥有项目的完整源代码。

2.  **安装 Maven**：
    如果您的系统中尚未安装 Maven，请先按照以下适用于您操作系统的步骤进行安装：

    *   **macOS (使用 Homebrew)**:
        1.  打开终端。
        2.  如果您尚未安装 Homebrew，请先访问 [Homebrew 官网](https://brew.sh/) 并按照指示安装。
            您可以用下面的命令查看自己是否安装了Homebrew:
            ```bash
                brew
            ```
        3.  执行以下命令安装 Maven：
            ```bash
            brew install maven
            ```

    *   **Windows (手动安装)**:
        1.  访问 [Apache Maven 官网下载页面](https://maven.apache.org/download.cgi)。
        2.  下载最新的二进制压缩包 (例如 `apache-maven-X.X.X-bin.zip` 或 `apache-maven-X.X.X-bin.tar.gz`)。
        3.  将下载的压缩包解压到您选择的目录 (例如 `C:\Program Files\Apache\maven` 或 `C:\maven`)。
        4.  设置环境变量：
            *   创建新的系统变量 `M2_HOME`，其值为您的 Maven 安装目录 (例如 `C:\Program Files\Apache\maven\apache-maven-X.X.X`)。
            *   创建新的系统变量 `MAVEN_HOME`，其值也为您的 Maven 安装目录。
            *   编辑系统的 `Path` 变量，在末尾添加 `%M2_HOME%\bin` (或 `%MAVEN_HOME%\bin`)。确保用分号与现有的路径隔开。
        5.  打开新的命令提示符窗口以使环境变量生效。

    安装完成后，请确保 `mvn` 命令在您的系统路径中可用。您可以通过在终端或新的命令提示符窗口中运行 `mvn -version` 来验证。如果显示了 Maven 的版本信息，则表示安装成功。

3.  **导航到项目根目录**：
    在终端或命令提示符中，进入项目的根目录 (即包含 `pom.xml` 文件的目录)。

4.  **使用 Maven 编译和打包**：
    执行以下 Maven 命令：

    ```bash
    mvn clean package
    ```
    此命令会首先清理任何旧的构建产物 (`clean`)，然后编译代码、运行测试（如果有），并将项目打包成一个可执行的 JAR 文件 (`package`)。

5.  **找到编译好的 JAR 文件**：
    命令成功执行后，编译好的 JAR 文件会位于项目根目录下的 `target/` 目录中。文件名通常会是 `json-db-1.0-SNAPSHOT.jar` (版本号可能因 `pom.xml` 配置而异)。

6.  **运行编译后的 JAR 文件**：
    您可以使用以下命令来运行您刚刚编译的应用程序：

    ```bash
    java -jar target/json-db-1.0-SNAPSHOT.jar
    ``` 

## 许可证 (License)

本项目采用 Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0) 许可证。
详情请见 [LICENSE](LICENSE) 文件或访问 [Creative Commons 网站](http://creativecommons.org/licenses/by-nc/4.0/)。

## 代码作者 (Contributors)
![Prelina Montelli](Prelina_Montelli.jpg)
*Prelina Montelli* |
*普利琳娜 · 莫塔里*

由Prelina Montelli完成，Cursor提供了部分代码编写。

© 2025 Prelina Montelli