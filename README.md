[![Build Status][build-status-img]][build-status]
[![Download][latest-img]][latest]

# SQLOb
SQLOb [_Sklob_], is a lightweight Java ORM library for persisting Java objects in a SQL database.

## Overview
### Session
The main outward-facing component of SQLOb is the `Session`.
A `Session` executes a `Request` using a `Connection` retrieved from its `DataSource` and returns a `Result`.
```java
// Class to persist
@Table("AClass")	// Custom name of the associated SQL table
class PersistedClass {
	@Transient	// Field which will not be persisted
	private int counter;
	@Column("AName")	// Custom name of the associated SQL column in the table
	private String name;
}
 
DataSource dataSource = getDataSource();	// Some JDBC data source
Session session = new Session(dataSource);	// What does the magic

try (session) {	// An individual Session is infinitely AutoCloseable
	PersistedClass obj = new PersistedClass(1, "anObj");	// Instance of class to persist
	
	Request<PersistedClass> insert = new InsertRequest<>(obj);	// Request to insert instance 'obj'
	Result<PersistedClass> insertResult = session.execute(insert);
	
	UUID key = insertResult.getKey().orElse(null);	// Generated key of the inserted instance
	
	Request<PersistedClass> select = new SelectRequest<>(PersistedClass.class, key);	// Request to select instance with key
	Result<PersistedClass> selectResult = session.execute(select)
	
	PersistedClass selectedObj = selectResult.getObject().orElse(null);	// Retrieved representation of 'obj'
} catch (UncheckedSqlException e) {
	session.rollback();
}
```

### Annotations
Several optional annotations may be used to customize persisted data.
* `@Transient` indicates that a field should be ignored by the persistence engine
* `@Table(String name)` sets the name of the table mapped to a persisted class (**DEFAULT**: simple name of the class)
* `@Column(String name)` sets the name of the column mapped to a persisted field (**DEFAULT**: name of the field)

### Logging
SQLOb has an optional dependency on the [SimpleLogs][simple-logs] library.
SQLOb logging can be activated by adding a `DEBUG`-level `Logger` to the `dev.kkorolyov.sqlob` logger hierarchy.

Further documentation found in the [Javadoc](https://kkorolyov.github.io/SQLOb).

## License
[BSD-new license](LICENSE).  

[build-status]: https://travis-ci.org/kkorolyov/SQLOb
[build-status-img]: https://travis-ci.org/kkorolyov/SQLOb.svg?branch=master
[latest]: https://bintray.com/kkorolyov/java/sqlob/_latestVersion
[latest-img]: https://api.bintray.com/packages/kkorolyov/java/sqlob/images/download.svg
[simple-logs]: https://github.com/kkorolyov/SimpleLogs
