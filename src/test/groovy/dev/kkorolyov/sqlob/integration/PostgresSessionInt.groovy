package dev.kkorolyov.sqlob.integration

import org.postgresql.ds.PGSimpleDataSource

import javax.sql.DataSource

class PostgresSessionInt extends SessionInt {
	protected DataSource buildDataSource() {
		PGSimpleDataSource ds = new PGSimpleDataSource()
		ds.setServerName("127.1")
		ds.setDatabaseName("sqlobtest")
		ds.setUser("postgres")

		return ds
	}
}
