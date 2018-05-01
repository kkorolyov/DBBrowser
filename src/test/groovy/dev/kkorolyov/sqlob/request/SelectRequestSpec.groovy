package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.column.FieldBackedColumn
import dev.kkorolyov.sqlob.util.Where

import spock.lang.Specification

import java.sql.PreparedStatement
import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class SelectRequestSpec extends Specification {
	class Stub {}
	Where where = Mock()
	FieldBackedColumn<?> column = Mock()

	PreparedStatement statement = Mock()
	ResultSet rs = Mock()
	ExecutionContext context = Mock()

	SelectRequest<?> request = new SelectRequest<>(Stub, where)

	def setup() {
		request.addColumn(column)
	}

	def "selects by where clause"() {
		when:
		request.execute(context)

		then:
		3 * where.getSql() >> randString()
		1 * where.toString() >> randString()
		1 * context.prepareStatement({ it.contains("SELECT") && it.contains("FROM ${Stub.getSimpleName()} WHERE ${where.getSql()}") }) >> statement
		1 * column.contributeToStatement(statement, where, context) >> statement
		1 * statement.executeQuery() >> rs
	}
}
