package dev.kkorolyov.sqlob

import com.mysql.cj.jdbc.MysqlDataSource
import dev.kkorolyov.simplelogs.Level
import dev.kkorolyov.simplelogs.append.Appenders
import dev.kkorolyov.simplelogs.format.Formatters
import org.postgresql.ds.PGSimpleDataSource
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import spock.lang.Shared
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement

import static dev.kkorolyov.sqlob.Stub.BasicStub
import static dev.kkorolyov.sqlob.Stub.SmartStub

class SessionPerf extends Specification {
	private static boolean CLEANUP = true

	@Shared int tests = 100

	@Shared DataSource sqliteDS = buildSQLiteDS()
	@Shared DataSource pgDS = buildPostgresDS()
	@Shared DataSource myDS = buildMySQLDS()

	@Shared DataSource[] dataSources = [sqliteDS]

	def setupSpec() {
		Logger.getLogger("dev.kkorolyov.sqlob", Level.DEBUG, Formatters.simple(), Appenders.err(Level.DEBUG))	// Enable logging
	}

	def cleanup() {
		if (CLEANUP) {
			dataSources.each {
				Connection conn = it.getConnection()
				conn.setAutoCommit(false)

				Statement s = conn.createStatement()
				for (String table : ['SmartStub', 'BasicStub', 'A', 'Test']) {
					String statement = "DROP TABLE IF EXISTS ${table}"
					s.addBatch(statement)
					s.addBatch(statement.toLowerCase())
				}
				s.executeBatch()

				conn.commit()
				conn.close()
			}
		}
	}

	def "test raw"() {
		Connection conn = ds.getConnection()
		conn.setAutoCommit(false)

		Statement s = conn.createStatement()
		s.executeUpdate("CREATE TABLE IF NOT EXISTS Test (id CHAR(3) PRIMARY KEY, num INT)")
		conn.commit()

		long start = System.nanoTime()
		PreparedStatement ps = conn.prepareStatement("INSERT INTO Test (id,num) VALUES (?,?)")
		(0..<tests).each {
			ps.setString(1, it as String)
			ps.setInt(2, it as int)
			ps.executeUpdate()
		}
		conn.commit()

		long ms = (System.nanoTime() - start) / 1000000
		println(ms + "ms to PUT ${tests} things using " + ds)

		start = System.nanoTime()
		ps = conn.prepareStatement("SELECT * FROM Test WHERE id = ?")
		(0..<tests).each {
			ps.setString(1, it as String)
			ps.executeQuery()
		}

		ms = (System.nanoTime() - start) / 1000000
		println(ms + "ms to GET ${tests} things using " + ds)

		conn.close()

		where:
		ds << dataSources
	}

	def "test BasicStub"() {
		long start = System.nanoTime()

		List<UUID> ids = []

		Session s = new Session(ds, tests)
		(0..<tests).each {
			ids.add(s.put(BasicStub.random()))
		}
		s.close()

		long ms = (System.nanoTime() - start) / 1000000
		println(ms + "ms to PUT ${tests} BasicStubs using " + ds)

		start = System.nanoTime()
		s = new Session(ds, tests)
		ids.each {
			s.get(BasicStub, it)
		}
		s.close()

		ms = (System.nanoTime() - start) / 1000000
		println(ms + "ms to GET ${tests} BasicStubs using " + ds)

		where:
		ds << dataSources
	}
	def "test SmartStub"() {
		long start = System.nanoTime()

		List<UUID> ids = []

		Session s = new Session(ds, tests)
		(0..<tests).each {
			ids.add(s.put(SmartStub.random()))
		}
		s.close()

		long ms = (System.nanoTime() - start) / 1000000
		println(ms + "ms to PUT ${tests} SmartStubs using " + ds)

		start = System.nanoTime()
		s = new Session(ds, tests)
		ids.each {
			s.get(SmartStub, it)
		}
		s.close()

		ms = (System.nanoTime() - start) / 1000000
		println(ms + "ms to GET ${tests} SmartStubs using " + ds)

		where:
		ds << dataSources
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
