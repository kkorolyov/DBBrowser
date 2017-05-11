[![Download](https://api.bintray.com/packages/kkorolyov/java/sqlob/images/download.svg) ](https://bintray.com/kkorolyov/java/sqlob/_latestVersion)

# SQLOb
SQLOb [_Sklob_], is a lightweight Java ORM library for persisting Java objects in a SQL database.

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
DataSource ds = <get a DataSource>;

MyPersistedClass toSave = new MyPersistedClass();
UUID savedInstanceId;

try (Session s = new Session(ds)) {
	savedInstanceId = s.put(toSave);
}
...
try (Session s = new Session(ds)) {
	MyPersistedClass retrieved = s.get(MyPersistedClass.class, savedInstanceId);
	assert retrieved.equals(toSave)
}
```

## Installation
[Click](https://bintray.com/kkorolyov/java/sqlob/_latestVersion) the pretty button at the top.

## Usage
### Session
A `Session` manages persistence between Java objects and a backing database.
A basic `Session` requires only a `DataSource` to a database.
`Session` provides various persistence methods:
##### Retrieval
* `UUID getId(Object o)` returns the ID of an instance persisted in the database
* `T get(Class<T> c, UUID id)` returns an instance of `T` persisted under an ID
* `Set<T> get(Class<T> c, Condition condition)` returns all persisted `T` instances matching a SQL-like condition

##### Insertion
* `UUID put(Object o)` persists an object in the backing database and returns the ID used to access it
* `boolean put(UUID id, Object o)` persists an object under a preset ID and returns `true` if this overwrites an earlier object referenced by the ID

##### Deletion
* `boolean drop(Class<?> c, UUID id)` deletes an instance of a class persisted under an ID and returns `true` if deletion was successful
* `int drop(Class<?> c, Condition condition)` deletes all instances of a class matching a SQL-like condition and returns the number of deletions

### Annotations
Several optional annotations may be used to customize persisted data.
* `@Transient` indicates that a field should be ignored by the persistence engine
* `@Table(String name)` sets the name of the table mapped to a persisted class (**DEFAULT**: simple name of the class)
* `@Column(String name)` sets the name of the column mapped to a persisted field (**DEFAULT**: name of the field)

### Mapping
Mapping between Java and SQL can be further customized by modifying the `Mapper` retrieved from a `Session` by `Session.getMapper()`.
A single mapping is defined as a 4-element tuple:
* Java class - `Class`
* SQL type - `String`
* Optional conversion function pre-processing/transforming an object before persisting it - `Converter`
* Extraction function converting a SQL result back to an object - `Extractor`

The default mappings can be used as an example:
```java
put(Byte.TYPE, "TINYINT", ResultSet::getByte);
put(Short.TYPE, "SMALLINT", ResultSet::getShort);
put(Integer.TYPE, "INTEGER", ResultSet::getInt);
put(Long.TYPE, "BIGINT", ResultSet::getLong);
put(Float.TYPE, "REAL", ResultSet::getFloat);
put(Double.TYPE, "DOUBLE", ResultSet::getDouble);

put(Boolean.TYPE, "BOOLEAN", ResultSet::getBoolean);

put(Character.TYPE, "CHAR(1)", (rs, column) -> {
	String string = rs.getString(column);
	return string == null ? null : string.charAt(0);
});

put(Byte.class, "TINYINT", ResultSet::getByte);
put(Short.class, "SMALLINT", ResultSet::getShort);
put(Integer.class, "INTEGER", ResultSet::getInt);
put(Long.class, "BIGINT", ResultSet::getLong);
put(Float.class, "REAL", ResultSet::getFloat);
put(Double.class, "DOUBLE", ResultSet::getDouble);
put(BigDecimal.class, "NUMERIC", ResultSet::getBigDecimal);

put(Boolean.class, "BOOLEAN", ResultSet::getBoolean);

put(Character.class, "CHAR(1)", (rs, column) -> {
	String string = rs.getString(column);
	return string == null ? null : string.charAt(0);
});
put(String.class, "VARCHAR(1024)", ResultSet::getString);

put(byte[].class, "VARBINARY(1024)", ResultSet::getBytes);

put(Date.class, "DATE", ResultSet::getDate);
put(Time.class, "TIME(6)", ResultSet::getTime);
put(Timestamp.class, "TIMESTAMP(6)", ResultSet::getTimestamp);

put(UUID.class, ID_TYPE, UUID::toString, (rs, column) -> {	// Store as string
	String string = rs.getString(column);
	return string == null ? null : UUID.fromString(string);
});
```

### Logging
SQLOb has an optional dependency on the [SimpleLogs](https://github.com/kkorolyov/SimpleLogs) library.
SQLOb logging can be activated by adding a `DEBUG`-level `Logger` to the `dev.kkorolyov.sqlob` logger hierarchy.

Further documentation found in the [Javadoc](https://kkorolyov.github.io/SQLOb).

## License
[BSD-new license](LICENSE).  
