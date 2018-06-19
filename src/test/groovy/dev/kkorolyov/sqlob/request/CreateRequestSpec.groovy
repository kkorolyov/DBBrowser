package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.Stub
import dev.kkorolyov.sqlob.result.ConfigurableResult
import dev.kkorolyov.sqlob.result.Result
import dev.kkorolyov.sqlob.statement.CreateStatementBuilder

import java.sql.Statement

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class CreateRequestSpec extends BaseRequestSpec<CreateRequest<?>> {
	CreateRequest<?> request = Spy(CreateRequest, constructorArgs: [Stub.BasicStub, randString(), columns])

	def "executes create statement"() {
		CreateStatementBuilder statementBuilder = Mock()
		Statement statement = Mock()

		when:
		Result<?> result = request.execute(context)

		then:
		1 * request.createBuilder(context) >> statementBuilder
		1 * statementBuilder.batch(_, _) >> statementBuilder
		1 * statementBuilder.build() >> statement
		1 * statement.executeBatch()
		result == new ConfigurableResult()
	}
}
