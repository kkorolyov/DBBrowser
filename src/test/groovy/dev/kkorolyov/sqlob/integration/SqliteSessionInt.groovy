package dev.kkorolyov.sqlob.integration

import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource

import javax.sql.DataSource

class SqliteSessionInt extends SessionInt {
	protected DataSource buildDataSource() {
		SQLiteConfig config = new SQLiteConfig()
		config.enforceForeignKeys(true)

		SQLiteDataSource ds = new SQLiteDataSource(config)
		ds.setUrl("jdbc:sqlite::memory:")

		return ds
	}
}
