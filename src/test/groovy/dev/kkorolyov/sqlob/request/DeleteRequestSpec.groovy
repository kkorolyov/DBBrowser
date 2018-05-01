package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.column.FieldBackedColumn
import dev.kkorolyov.sqlob.util.Where

import spock.lang.Specification

import java.sql.PreparedStatement

class DeleteRequestSpec extends Specification {
	class Stub {}
	Where where = Mock()
	FieldBackedColumn<?> column = Mock()

	PreparedStatement statement = Mock()
	ExecutionContext context = Mock()

	DeleteRequest<?> request = new DeleteRequest(Stub, where)

	def setup() {
		request.addColumn(column)
	}

	def "deletes by where clause"() {
		when:
		request.execute(context)

		then:
		1 * context.prepareStatement("DELETE FROM ${Stub.getSimpleName()} WHERE ${where.getSql()}") >> statement
		1 * column.contributeToStatement(statement, where, context) >> statement
		1 * statement.executeUpdate()
	}
}
