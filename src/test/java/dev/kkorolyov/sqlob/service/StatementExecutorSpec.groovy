package dev.kkorolyov.sqlob.service

import spock.lang.Shared
import spock.lang.Specification

import java.sql.Connection
import java.sql.Statement

class StatementExecutorSpec extends Specification {
  @Shared Mapper mapper = Mock()
	Statement statement = Mock()
	Connection conn = Mock()

	StatementExecutor executor = new StatementExecutor(mapper)

	def setup() {
		executor.setConnection(conn)
	}

	def "create() batch executes all statements in order"() {
		1 * conn.createStatement() >> statement

		Iterable<String> creates = ["0", "1", "2", "3"]

		when:
		executor.create(creates)

		then:
		1 * statement.addBatch(creates[0])
		then:
		1 * statement.addBatch(creates[1])
		then:
		1 * statement.addBatch(creates[2])
		then:
		1 * statement.addBatch(creates[3])
		then:
		1 * statement.executeBatch()
	}
}
