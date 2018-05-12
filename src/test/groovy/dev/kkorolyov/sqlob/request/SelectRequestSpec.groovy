package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.Stub
import dev.kkorolyov.sqlob.column.KeyColumn
import dev.kkorolyov.sqlob.result.Result
import dev.kkorolyov.sqlob.util.Where

import java.sql.PreparedStatement
import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.randString
import static dev.kkorolyov.simplespecs.SpecUtilities.setField

class SelectRequestSpec extends BaseRequestSpec<SelectRequest<?>> {
	KeyColumn idColumn = Mock()
	Where where

	def setup() {
		setField("ID", KeyColumn, idColumn)
	}

	@Override
	SelectRequest<?> buildRequest() {
		where = Where.eq(randString(), randString())

		return new SelectRequest<>(Stub.BasicStub, where)
	}

	def "selects by where clause"() {
		PreparedStatement statement = Mock()
		ResultSet rs = Mock()

		UUID id = UUID.randomUUID()

		when:
		Result<?> result = request.execute(context)

		then:
		1 * context.generateStatement({ it.contains("SELECT") && it.contains("FROM ${request.name} WHERE ${where.sql}") }) >> statement
		columns.each {
			1 * it.contribute(statement, where, context) >> statement
			1 * it.contribute(_, rs, context)
		}
		1 * statement.executeQuery() >> rs
		2 * rs.next() >> true >> false
		1 * idColumn.getValue(rs, context) >> id
		result.getId().orElse(null) == id
		result.getObject().orElse(null) instanceof Stub.BasicStub
	}
}
