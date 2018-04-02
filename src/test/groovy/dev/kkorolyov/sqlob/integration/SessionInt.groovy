package dev.kkorolyov.sqlob.integration

import dev.kkorolyov.simplelogs.Level
import dev.kkorolyov.simplelogs.Logger
import dev.kkorolyov.simplelogs.append.Appenders
import dev.kkorolyov.simplelogs.format.Formatters
import dev.kkorolyov.sqlob.Session
import dev.kkorolyov.sqlob.request.DeleteRequest
import dev.kkorolyov.sqlob.request.InsertRequest
import dev.kkorolyov.sqlob.request.SelectRequest

import spock.lang.Shared
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.Statement

import static dev.kkorolyov.sqlob.Stub.BasicStub
import static dev.kkorolyov.sqlob.Stub.SmartStub

abstract class SessionInt extends Specification {
	@Shared BasicStub bs = BasicStub.random()
	@Shared SmartStub ss = SmartStub.random()

	@Shared DataSource dataSource = buildDataSource();

	Session session = new Session(dataSource)

	protected abstract DataSource buildDataSource()

	def setupSpec() {
		Logger.getLogger("", Level.DEBUG, Formatters.simple(), Appenders.err(Level.DEBUG))
	}

	def cleanup() {
		session.close()

		Connection conn = dataSource.getConnection()

		Statement statement = conn.createStatement()
		['BasicStub', 'SmartStub'].each {
			String sql = "DROP TABLE IF EXISTS $it"
			statement.addBatch(sql)
			statement.addBatch(sql.toLowerCase())
		}
		statement.executeBatch()

		conn.close()
	}

	def "inserts and selects"() {
		when:
		UUID bsId = insert(bs)
		UUID ssId = insert(ss)

		then:
		select(BasicStub, bsId) == bs
		select(SmartStub, ssId) == ss
	}

	def "inserts and deletes"() {
		when:
		UUID bsId = insert(bs)
		UUID ssId = insert(ss)
		delete(BasicStub, bsId)
		delete(SmartStub, ssId)

		then:
		!select(BasicStub, bsId)
		!select(SmartStub, ssId)
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
}
