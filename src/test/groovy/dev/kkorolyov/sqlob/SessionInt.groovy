package dev.kkorolyov.sqlob

import com.mysql.cj.jdbc.MysqlDataSource
import dev.kkorolyov.simplelogs.Level
import dev.kkorolyov.simplelogs.Logger
import dev.kkorolyov.simplelogs.append.Appenders
import dev.kkorolyov.simplelogs.format.Formatters
import dev.kkorolyov.sqlob.request.DeleteRequest
import dev.kkorolyov.sqlob.request.InsertRequest
import dev.kkorolyov.sqlob.request.SelectRequest
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

	@Shared BasicStub bs = BasicStub.random()
	@Shared SmartStub ss = SmartStub.random()

	Session session

	def setupSpec() {
		Logger.getLogger("", Level.DEBUG, Formatters.simple(), Appenders.err(Level.DEBUG))
	}

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

	def "inserts and selects"() {
		prepareSession(ds)

		when:
		UUID bsId = insert(bs)
		UUID ssId = insert(ss)

		then:
		select(BasicStub, bsId) == bs
		select(SmartStub, ssId) == ss

		where:
		ds << dataSources
	}

	def "inserts and deletes"() {
		prepareSession(ds)

		when:
		UUID bsId = insert(bs)
		UUID ssId = insert(ss)
		delete(BasicStub, bsId)
		delete(SmartStub, ssId)

		then:
		!select(BasicStub, bsId)
		!select(SmartStub, ssId)

		where:
		ds << dataSources
	}

	private <T> T select(Class<T> c, UUID id) {
		return session.execute(new SelectRequest<>(c, id)).getObject().orElse(null)
	}
	private UUID insert(Object o) {
		return session.execute(new InsertRequest<>(o)).getId().orElse(null)
	}
	private void delete(Class<?> c, UUID id) {
		session.execute(new DeleteRequest<>(c, id))
	}

	private void prepareSession(DataSource ds) {
		session = new Session(ds)
	}

	private static DataSource buildSQLiteDS() {
		SQLiteConfig config = new SQLiteConfig()
		config.enforceForeignKeys(true)

		SQLiteDataSource ds = new SQLiteDataSource(config)
		ds.setUrl("jdbc:sqlite::memory:")

		return ds
	}
	private static DataSource buildPostgresDS() {
		PGSimpleDataSource ds = new PGSimpleDataSource()
		ds.setServerName("127.1")
		ds.setDatabaseName("sqlobtest")
		ds.setUser("postgres")

		return ds
	}
	private static DataSource buildMySQLDS() {
		MysqlDataSource ds = new MysqlDataSource()
		ds.setUrl("jdbc:mysql://127.1/sqlobtest?useLegacyDatetimeCode=false&serverTimezone=America/Los_Angeles")
		ds.setUser("travis")

		return ds
	}
}
