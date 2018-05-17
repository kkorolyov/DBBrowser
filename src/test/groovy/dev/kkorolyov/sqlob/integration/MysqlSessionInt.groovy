package dev.kkorolyov.sqlob.integration

import com.mysql.cj.jdbc.MysqlDataSource

import javax.sql.DataSource

class MysqlSessionInt extends SessionInt {
	protected DataSource buildDataSource() {
		MysqlDataSource ds = new MysqlDataSource()
		ds.setUrl("jdbc:mysql://127.1/sqlobtest?useLegacyDatetimeCode=false")
		ds.setUser("travis")

		return ds
	}
}
