package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.util.Where

import spock.lang.Specification

import java.sql.Connection
import java.sql.PreparedStatement

class DeleteRequestSpec extends Specification {
	Class<?> type = String

	PreparedStatement statement = Mock()
	Connection connection = Mock()
	ExecutionContext context = Mock() {
		getConnection() >> connection
	}
	Where where = Mock()

	DeleteRequest<?> request = new DeleteRequest(type, where)

	def "deletes by where clause"() {
		when:
		request.executeInContext(context)

		then:
		1 * connection.prepareStatement("DELETE FROM ${type.getSimpleName()} WHERE $where") >> statement
		1 * where.contributeToStatement(statement) >> statement
		1 * statement.executeUpdate()
	}
}
