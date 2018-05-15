package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.Stub
import dev.kkorolyov.sqlob.result.Result
import dev.kkorolyov.sqlob.util.Where

import java.sql.PreparedStatement
import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class SelectRequestSpec extends BaseRequestSpec<SelectRequest<?>> {
	Where where = Where.eq(randString(), randString())

	SelectRequest<?> request = new SelectRequest<>(Stub.BasicStub, randString(), where, columns)

	def "selects by where clause"() {
		PreparedStatement statement = Mock()
		ResultSet rs = Mock()

		UUID key = UUID.randomUUID()

		when:
		Result<?> result = request.execute(context)

		then:
		1 * context.generateStatement({ it.contains("SELECT") && it.contains("FROM ${request.name} WHERE ${where.sql}") }) >> statement
		columns.each {
			1 * it.contribute(statement, where, context) >> statement
			1 * it.contribute(_, rs, context) >> { record, rs1, context ->
				record.key = key
				record
			}
		}
		1 * statement.executeQuery() >> rs
		2 * rs.next() >> true >> false
		result.key.orElse(null) == key
		result.object.orElse(null) instanceof Stub.BasicStub
	}
}
