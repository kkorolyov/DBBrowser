package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.column.FieldBackedColumn
import dev.kkorolyov.sqlob.column.handler.ReferencingColumnHandler

import spock.lang.Specification

import java.sql.Statement

class CreateRequestSpec extends Specification {
	Class<?> type = Byte
	Class<?> colType = String
	Class<?> refColType = Integer

	FieldBackedColumn<String> col = Mock() {
		getType() >> colType
		getName() >> colType.getSimpleName()
	}
	ReferencingColumnHandler.ReferencingColumn refCol = Mock() {
		getType() >> refColType
		getReferencedName() >> refColType.getSimpleName()
	}
	ReferencingColumnHandler.ReferencingColumn refCol1 = Mock() {
		getType() >> refColType
		getReferencedName() >> refColType.getSimpleName()
	}

	Statement statement = Mock()
	ExecutionContext context = Mock() {
		getStatement() >> statement
	}

	CreateRequest<?> request = new CreateRequest<>(type)

	def "batch executes create table statements for self and each distinct referencing column type"() {
		when:
		request.addColumn(col)
		request.addColumn(refCol)
		request.addColumn(refCol1)

		request.execute(context)

		then:
		1 * statement.addBatch({ it.contains("CREATE TABLE IF NOT EXISTS ${type.getSimpleName()}") })
		1 * statement.addBatch({ it.contains("CREATE TABLE IF NOT EXISTS ${refColType.getSimpleName()}") })
		1 * statement.executeBatch()
	}
}
