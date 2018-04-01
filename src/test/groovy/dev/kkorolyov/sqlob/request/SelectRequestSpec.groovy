package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.util.Where

import spock.lang.Specification

import java.sql.PreparedStatement
import java.sql.ResultSet

class SelectRequestSpec extends Specification {
	Class<?> type = String
	Where where = Mock()

	PreparedStatement statement = Mock()
	ResultSet rs = Mock()
	ExecutionContext context = Mock()

	SelectRequest<?> request = new SelectRequest<>(type, where)

	def "selects by where clause"() {
		when:
		request.execute(context)

		then:
		1 * context.prepareStatement({ it.contains("SELECT") && it.contains("FROM ${type.getSimpleName()} WHERE $where") }) >> statement
		1 * where.contributeToStatement(statement) >> statement
		1 * statement.executeQuery() >> rs
	}
}
