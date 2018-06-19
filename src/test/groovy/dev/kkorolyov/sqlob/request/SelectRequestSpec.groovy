package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.Stub
import dev.kkorolyov.sqlob.result.Result
import dev.kkorolyov.sqlob.statement.SelectStatementBuilder
import dev.kkorolyov.sqlob.util.Where

import java.sql.PreparedStatement
import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class SelectRequestSpec extends BaseRequestSpec<SelectRequest<?>> {
	Where where = Where.eq(randString(), randString())

	SelectRequest<?> request = Spy(SelectRequest, constructorArgs: [Stub.BasicStub, randString(), where, columns])

	def "executes select statement"() {
		SelectStatementBuilder statementBuilder = Mock()
		PreparedStatement statement = Mock()
		ResultSet rs = Mock()

		UUID key = UUID.randomUUID()
		Object object = Mock()

		when:
		Result<?> result = request.execute(context)

		then:
		1 * request.selectBuilder(context) >> statementBuilder
		1 * statementBuilder.build() >> statement
		1 * statement.executeQuery() >> rs
		2 * rs.next() >> true >> false
		columns.each {
			1 * it.set(_, rs, context) >> { record, rs1, context ->
				record.key = key
				record.object = object
				record
			}
		}
		result.key.orElse(null) == key
		result.object.orElse(null) == object
	}
}
