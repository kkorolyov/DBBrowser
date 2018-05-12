package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.Stub

import java.sql.PreparedStatement

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class CreateRequestSpec extends BaseRequestSpec<CreateRequest<?>> {
	CreateRequest<?> request = new CreateRequest<>(Stub.BasicStub, randString(), columns)

	def "batch executes statement"() {
		PreparedStatement statement = Mock()

		when:
		request.execute(context)

		then:
		1 * columnHandler.expandCreates(request) >> [request].stream()
		columns.each { 1 * it.getSql(context) }
		1 * context.generateStatement() >> statement
		1 * statement.addBatch({ it.contains("CREATE TABLE IF NOT EXISTS ${request.name}") })
		1 * statement.executeBatch()
	}
}
