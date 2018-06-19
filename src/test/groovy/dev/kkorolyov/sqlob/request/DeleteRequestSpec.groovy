package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.Stub
import dev.kkorolyov.sqlob.result.ConfigurableResult
import dev.kkorolyov.sqlob.result.Result
import dev.kkorolyov.sqlob.statement.DeleteStatementBuilder
import dev.kkorolyov.sqlob.util.Where

import java.sql.PreparedStatement

import static dev.kkorolyov.simplespecs.SpecUtilities.randInt
import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class DeleteRequestSpec extends BaseRequestSpec<DeleteRequest<?>> {
	Where where = Where.eq(randString(), randString()) // Yay for non-initialized fields at this point

	DeleteRequest<?> request = Spy(DeleteRequest, constructorArgs: [Stub.BasicStub, randString(), where, columns])

	def "executes delete statement"() {
		DeleteStatementBuilder statementBuilder = Mock()
		PreparedStatement statement = Mock()
		int deleted = randInt()

		when:
		Result<?> result = request.execute(context)

		then:
		1 * request.deleteBuilder(context) >> statementBuilder
		1 * statementBuilder.build() >> statement
		1 * statement.executeUpdate() >> deleted
		result == new ConfigurableResult().size(deleted)
	}
}
