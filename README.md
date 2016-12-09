# SQLOb
SQLOb [_Sklob_], is a Java library providing for persistence of Java objects in a SQL database.

## Examples
```java
public class MyPersistedClass {
	@Transient
	private int counter;
	private String name;
	@Column("")
	MyOtherClass other;
	
	public MyPersistedClass() {...}
}
...
MyPersistedClass toSave = new MyPersistedClass();
DataSource ds = <_get a DataSource_>;

try (Session s = new Session(ds)) {
	UUID savedInstanceId = s.put(toSave);	// Saves to database
	s.flush();	// Commits requests and flushes buffer
	MyPersistedClass retrieved = s.get(MyPersistedClass.class, savedInstanceId);	// Retrieves from database
}
```

## Installation
* Download the [latest release](https://github.com/kkorolyov/SQLOb/releases/latest).
* Add either the source or bundled .jar file to your project's classpath.

## Usage
### Session
A `Session` manages persistence between Java objects and a backing database.
A basic `Session` requires only a `DataSource` to a database.
`Session` provides various persistence methods:
* `UUID getId(Object o)` returns the ID of an instance persisted in the database
* `T get(Class<T> c, UUID id)` returns an instance of `T` persisted under an ID
* 

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
