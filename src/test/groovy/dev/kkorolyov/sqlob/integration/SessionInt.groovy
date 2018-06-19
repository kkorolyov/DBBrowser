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
	@Shared DataSource dataSource = buildDataSource()

	BasicStub bs = BasicStub.random()
	SmartStub ss = SmartStub.random()

	Session session = new Session(dataSource)

	protected abstract DataSource buildDataSource()

	def setupSpec() {
		Logger.getLogger("", Level.DEBUG, Formatters.simple(), Appenders.err(Level.DEBUG))
	}

	def cleanup() {
		session.close()

		Connection conn = dataSource.getConnection()

		Statement statement = conn.createStatement()
		['SmartStub', 'BasicStub'].each {
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
	def "inserts and updates"() {
		when:
		UUID bsId = insert(bs)
		UUID ssId = insert(ss)

		bs = BasicStub.random()
		ss = SmartStub.random()

		then:
		insert(bs, bsId) == bsId
		insert(ss, ssId) == ssId
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

	def "rolls back changes"() {
		when:
		insert(BasicStub.random())
		insert(SmartStub.random())
		session.close()	// Commit table creations

		UUID bsId = insert(bs)
		UUID ssId = insert(ss)
		session.rollback()

		then:
		!select(BasicStub, bsId)
		!select(SmartStub, ssId)
	}

	private <T> T select(Class<T> c, UUID id) {
		session.execute(new SelectRequest<>(c, id)).object.orElse(null)
	}
	private UUID insert(Object o, UUID id = UUID.randomUUID()) {
		session.execute(new InsertRequest<>(id, o)).key.orElse(null)
	}
	private void delete(Class<?> c, UUID id) {
		session.execute(new DeleteRequest<>(c, id))
	}
}
