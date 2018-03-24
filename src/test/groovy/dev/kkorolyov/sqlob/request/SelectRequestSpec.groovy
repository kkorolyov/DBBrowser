package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.util.Where

import spock.lang.Specification

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class SelectRequestSpec extends Specification {
	Class<?> type = String
	Where where = Mock()

	PreparedStatement statement = Mock()
	ResultSet rs = Mock()
	Connection connection = Mock()
	ExecutionContext context = Mock() {
		getConnection() >> connection
	}

	SelectRequest<?> request = new SelectRequest<>(type, where)

	def "selects by where clause"() {
		when:
		request.executeInContext(context)

		then:
		1 * connection.prepareStatement({ it.contains("SELECT") && it.contains("FROM ${type.getSimpleName()} WHERE $where") }) >> statement
		1 * where.contributeToStatement(statement) >> statement
		1 * statement.executeQuery() >> rs
	}
}
