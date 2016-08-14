# SQLOb
SQLOb (pronounced "Sklob"), is a Java API facilitating object-oriented access to a SQL database.

## Examples
```java
String 	host = "0.0.0.0",
				database = "sqlob_db",
				user = "fakeuser",
				password = "realpass";
DatabaseType databaseType = DatabaseType.POSTGRESQL;

DatabaseConnection dbConn = new DatabaseConnection(host, database, databaseType, user, password);
String createTableBaseStatement = "CREATE TABLE new_table (name VARCHAR, is_useless BOOLEAN, uselessness_scale INTEGER)";

dbConn.update(createTableBaseStatement, (RowEntry[]) null);	// Executes update with no parameters

String tableName = "sqlob_table";
if (dbConn.containsTable(tableName)) {
	TableConnection tableConn = dbConn.connect(tableName);
	
	Results allData = tableConn.select();	// Selects all data in table
}
```

## Installation
* Download the [latest release](https://github.com/kkorolyov/SQLOb/releases/latest).
* Add either the source or bundled .jar file to your project's classpath.

## Usage
### DatabaseConnection
A `DatabaseConnection` is a connection to a single database.
The parameters required by a `DatabaseConnection` include:
* Database host address
* Database name
* Database user's username
* Database user's password
* Database type

SQL statements can be executed on the selected database using any of the following methods:
* `DatabaseConnection#execute(QueryStatement):Results`
* `DatabaseConnection#execute(UpdateStatement):int`
* `DatabaseConnection#execute(String, RowEntry...):Results`
* `DatabaseConnection#update(Strings, RowEntry...):int`

### TableConnection
A `TableConnection` is a connection to a single table on a database.
A `TableConnection` to any table on a database may be retrieved using the `DatabaseConnection#connect(String)` method, which accepts the name of the table to connect to as the only parameter.

### StatementCommand
A `StatementCommand` is a complete SQL statement ready for execution.
It consists of:
* The base SQL statement, specified as a `String` with `?` characters denoting areas for parameter substitution during execution
* A set of `RowEntry` objects denoting the database values set or modified by the statement
* A set of `RowEntry` objects denoting the criteria restricting the statement
* The `DatabaseConnection` used to execute the statement

A `StatementCommand` is created using one of the multiple methods found in `StatementFactory`.
Each `DatabaseConnection` has its own `StatementFactory`, which may be retrieved using the `DatabaseConnection#getStatementFactory()` method.

### Results
Whereas update statements return an `int` of the number of rows added, removed, or otherwise modified, query statements return a `Results` object.
`Results` is similar to the java.sql package's `ResultSet` in that when freshly-created, `Results` has a cursor immediately preceeding the first row of data returned by the SQL query, as well as providing a method (Results#getNextRow():RowEntry[]) used to advance this cursor one row and return the row of data at that row.

### Logging
SQLOb has an optional dependency on the [SimpleLogs](https://github.com/kkorolyov/SimpleLogs) library.
If SimpleLogs is found on the classpath during runtime, SQLOb will log messages at the DEBUG level using the methods provided by SimpleLogs.

## License
BSD-new license.  
More detail found [here](LICENSE).
