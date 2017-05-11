package dev.kkorolyov.sqlob.utility

import com.mysql.cj.jdbc.MysqlDataSource
import dev.kkorolyov.sqlob.Session
import groovy.transform.CompileStatic
import org.postgresql.ds.PGSimpleDataSource
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import spock.lang.Shared
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.Statement

import static dev.kkorolyov.sqlob.Stub.BasicStub
import static dev.kkorolyov.sqlob.Stub.SmartStub

class SessionInt extends Specification {
	@Shared DataSource sqliteDS = buildSQLiteDS()
	@Shared DataSource pgDS = buildPostgresDS()
	@Shared DataSource myDS = buildMySQLDS()

	@Shared DataSource[] dataSources = [sqliteDS]

	@Shared Condition allCondition = null
	@Shared BasicStub bs = BasicStub.random()
	@Shared SmartStub ss = SmartStub.random()

	Session session

	def cleanup() {
		session.close()

		dataSources.each {
			Connection conn = it.getConnection()

			Statement statement = conn.createStatement()
			['BasicStub', 'SmartStub'].each {
				String sql = "DROP TABLE IF EXISTS $it"
				statement.addBatch(sql)
				statement.addBatch(sql.toLowerCase())
			}
			statement.executeBatch()

			conn.close()
		}
	}

	def "gets by ID"() {
		prepareSession(ds)

		when:
		UUID bsId = session.put(bs)
		UUID ssId = session.put(ss)

		then:
		session.get(BasicStub, bsId) == bs
		session.get(SmartStub, ssId) == ss

		where:
		ds << dataSources
	}
	def "gets by condition"() {
		prepareSession(ds)

		when:
		session.put(bs)
		session.put(ss)

		then:
		session.get(BasicStub, new Condition('short0', '=', bs.getShort0())).values()[0] == bs
		session.get(SmartStub, new Condition('stub', '=', ss.getStub())).values()[0] == ss

		where:
		ds << dataSources
	}

	def "gets ID"() {
		prepareSession(ds)

		when:
		UUID bsId = session.put(bs)
		UUID ssId = session.put(ss)

		then:
		session.getId(bs) == bsId
		session.getId(ss) == ssId

		where:
		ds << dataSources
	}

	def "put with ID updates"() {
		prepareSession(ds)

		BasicStub bs2 = BasicStub.random()
		SmartStub ss2 = SmartStub.random()

		when:
		UUID bsId = session.put(bs)
		UUID ssId = session.put(ss)

		then:
		session.get(BasicStub, bsId) == bs
		session.get(SmartStub, ssId) == ss

		when:
		session.put(bsId, bs2)
		session.put(ssId, ss2)

		then:
		session.get(BasicStub, bsId) == bs2
		session.get(SmartStub, ssId) == ss2

		where:
		ds << dataSources
	}

	def "drops by ID"() {
		prepareSession(ds)

		when:
		UUID bsId = session.put(bs)
		UUID ssId = session.put(ss)

		then:
		getAll(BasicStub).size() == 2
		getAll(SmartStub).size() == 1

		when:
		session.drop(BasicStub, bsId)
		session.drop(SmartStub, ssId)

		then:
		getAll(BasicStub).size() == 1
		getAll(SmartStub).size() == 0

		where:
		ds << dataSources
	}
	def "drops by Condition"() {
		prepareSession(ds)

		when:
		session.put(bs)
		session.put(ss)

		then:
		getAll(BasicStub).size() == 2
		getAll(SmartStub).size() == 1

		when:
		dropAll(BasicStub)
		dropAll(SmartStub)

		then:
		getAll(BasicStub).size() == 0
		getAll(SmartStub).size() == 0

		where:
		ds << dataSources
	}

	@CompileStatic
	private <T> Map<UUID, T> getAll(Class<T> c) {
		return session.get(c, allCondition)
	}
	@CompileStatic
	private <T> void dropAll(Class<T> c) {
		session.drop(c, allCondition)
	}

	private void prepareSession(DataSource ds) {
		session = new Session(ds)
	}

	private static DataSource buildSQLiteDS() {
		String sqliteFile = "M:\\test\\sqlite.db"
		new File(sqliteFile).delete()

		SQLiteConfig config = new SQLiteConfig()
		config.enforceForeignKeys(true)

		SQLiteDataSource ds = new SQLiteDataSource(config)
		ds.setUrl("jdbc:sqlite:${sqliteFile}")

		return ds
	}
	private static DataSource buildPostgresDS() {
		PGSimpleDataSource ds = new PGSimpleDataSource()
		ds.setServerName("192.168.1.195")
		ds.setDatabaseName("sqlobtest")
		ds.setUser("sqlob")
		ds.setPassword("Password1!")

		return ds
	}
	private static DataSource buildMySQLDS() {
		MysqlDataSource ds = new MysqlDataSource()
		ds.setUrl("jdbc:mysql://192.168.1.195/sqlobtest?useLegacyDatetimeCode=false&serverTimezone=America/Los_Angeles")
		ds.setUser("sqlob")
		ds.setPassword("Password1!")

		return ds
	}
}
