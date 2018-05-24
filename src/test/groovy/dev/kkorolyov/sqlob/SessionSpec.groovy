package dev.kkorolyov.sqlob

import dev.kkorolyov.simplefiles.Providers
import dev.kkorolyov.sqlob.column.FieldBackedColumn
import dev.kkorolyov.sqlob.column.handler.ColumnHandler
import dev.kkorolyov.sqlob.column.handler.factory.ColumnHandlerFactory
import dev.kkorolyov.sqlob.request.CreateRequest
import dev.kkorolyov.sqlob.request.Request
import dev.kkorolyov.sqlob.result.Result

import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection

import static dev.kkorolyov.simplespecs.SpecUtilities.getField
import static dev.kkorolyov.simplespecs.SpecUtilities.setField

class SessionSpec extends Specification {
	DataSource dataSource = Mock()
	Connection connection = Mock()

	ColumnHandler columnHandler = Mock(ColumnHandler) {
		accepts(_) >> true
		get(_) >> Mock(FieldBackedColumn)
	}.with {
		setField("COLUMN_FACTORIES", ColumnHandlerFactory, Providers.fromInstances(ColumnHandler, [it] as Set))
		it
	}

	Class<?> type = String
	Request request = Spy(Request, constructorArgs: [type]) {
		executeThrowing(_) >> null
	}

	Session session = Spy(Session, constructorArgs: [dataSource]).with {
		(getField("prepared", Session, it) as Set<Class<?>>).add(type)
		it
	}

	def "creates necessary tables, executes request with current connection"() {
		(getField("prepared", Session, session) as Set<Class<?>>).clear()

		CreateRequest<?> createRequest = Mock()
		Result<?> expected = Mock()

		when:
		Result<?> result = session.execute(request)

		then:
		1 * dataSource.getConnection() >> connection
		1 * session.create(type) >> createRequest
		1 * createRequest.executeThrowing(_ as ExecutionContext)
		1 * request.executeThrowing(_ as ExecutionContext) >> expected
		result == expected
	}
	def "does not create table if already prepared"() {
		Result<?> expected = Mock()

		when:
		Result<?> result = session.execute(request)

		then:
		1 * dataSource.getConnection() >> connection
		0 * session.create(_)
		1 * request.executeThrowing(_ as ExecutionContext) >> expected
		result == expected
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
