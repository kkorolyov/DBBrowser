package dev.kkorolyov.sqlob

import dev.kkorolyov.sqlob.request.Request
import dev.kkorolyov.sqlob.result.Result

import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.Statement

import static dev.kkorolyov.simplespecs.SpecUtilities.getField

class SessionSpec extends Specification {
	DataSource dataSource = Mock()
	Connection connection = Mock()
	ExecutionContext context = new ExecutionContext(connection)

	Class<?> type = String
	Request request = Spy(constructorArgs: [type]) {
		executeThrowing(_) >> null
	}

	Session session = new Session(dataSource).with {
		(getField("prepared", it) as Set<Class<?>>).add(type)
		it
	}

	def "creates necessary tables, executes request with current connection"() {
		Statement statement = Mock()
		Result<?> expected = Mock()
		(getField("prepared", session) as Set<Class<?>>).clear()

		when:
		Result<?> result = session.execute(request)

		then:
		1 * dataSource.getConnection() >> connection
		1 * connection.createStatement() >> statement
		1 * statement.addBatch({ it.contains("CREATE TABLE IF NOT EXISTS ${type.getSimpleName()}") })
		1 * request.executeThrowing(_) >> expected
		result == expected
	}
	def "does not create table if already prepared"() {
		when:
		session.execute(request)

		then:
		1 * dataSource.getConnection() >> connection
		0 * connection.createStatement()
	}

	def "rolls back connection if has connection"() {
		when:
		session.execute(request)
		session.rollback()

		then:
		1 * dataSource.getConnection() >> connection
		1 * connection.rollback()
	}
	def "does nothing on rollback if no connection"() {
		when:
		session.rollback()

		then:
		0 * connection.rollback()
	}

	def "commits on close if has connection"() {
		when:
		session.execute(request)
		session.close()

		then:
		1 * dataSource.getConnection() >> connection
		1 * connection.close()
	}
	def "does nothing on close if no connection"() {
		when:
		session.close()

		then:
		0 * connection.close()
	}
}
