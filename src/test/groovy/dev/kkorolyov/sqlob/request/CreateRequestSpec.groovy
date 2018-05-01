package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.ExecutionContext

import spock.lang.Specification

import java.sql.DatabaseMetaData
import java.sql.Statement

class CreateRequestSpec extends Specification {
	class Stub {}
	String database = "SQLite"
	DatabaseMetaData metaData = Mock()
	Statement statement = Mock()
	ExecutionContext context = Mock()

	CreateRequest<?> request = new CreateRequest<>(Stub)

	def "batch executes statement"() {
		when:
		request.execute(context)

		then:
		2 * metaData.getDatabaseProductName() >> database
		2 * context.getMetadata() >> metaData
		1 * context.getStatement() >> statement
		1 * statement.executeBatch()
	}
}
