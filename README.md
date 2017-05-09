[![Download](https://api.bintray.com/packages/kkorolyov/java/sqlob/images/download.svg) ](https://bintray.com/kkorolyov/java/sqlob/_latestVersion)

# SQLOb
SQLOb [_Sklob_], is a Java library providing for persistence of Java objects in a SQL database.

Version 2.0 completely overhauled the project, turning it away from an additional wrapper on top of JDBC and into a more focused persistence library.

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
DataSource ds = <get a DataSource>;

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
* `@Transient` indicates that field will be ignored by the persistence engine
* `@Table(String name)` overrides the name of the table mapped to a persisted class, which defaults to the simple name of the class
* `@Column(String name)` overrides the name of the column mapped to a persisted field, which defaults to the name of the field

### Logging
SQLOb has an optional dependency on the [SimpleLogs](https://github.com/kkorolyov/SimpleLogs) library.
SQLOb logging can be activated by adding a `DEBUG`-level `Logger` to the `dev.kkorolyov.sqlob` logger hierarchy.

## License
BSD-new license.  
More detail found [here](LICENSE).
