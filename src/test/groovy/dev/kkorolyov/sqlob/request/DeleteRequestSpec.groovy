package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.util.Where

import spock.lang.Specification

import java.sql.PreparedStatement

class DeleteRequestSpec extends Specification {
	Class<?> type = String
	Where where = Mock()

	PreparedStatement statement = Mock()
	ExecutionContext context = Mock()

	DeleteRequest<?> request = new DeleteRequest(type, where)

	def "deletes by where clause"() {
		when:
		request.execute(context)

		then:
		1 * context.prepareStatement("DELETE FROM ${type.getSimpleName()} WHERE ${where.getSql()}") >> statement
		1 * where.contributeToStatement(statement) >> statement
		1 * statement.executeUpdate()
	}
}
