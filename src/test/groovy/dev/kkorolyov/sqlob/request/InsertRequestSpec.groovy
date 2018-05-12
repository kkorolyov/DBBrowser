package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.Stub
import dev.kkorolyov.sqlob.result.Record
import dev.kkorolyov.sqlob.result.Result

import java.sql.PreparedStatement

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class InsertRequestSpec extends BaseRequestSpec<InsertRequest<?>> {
	Collection<Record<?, ?>> records = (0..5).collect { new Record<>(UUID.randomUUID(), Stub.BasicStub.random()) }

	InsertRequest<?> request = Spy(InsertRequest, constructorArgs: [records, randString(), columns])

	def "inserts non-existent records"() {
		def (Collection<Record<?>> existent, Collection<Record<?>> remaining) = records.collate(records.size() / 2 as int)

		SelectRequest<?> innerSelect = Mock()
		Result<?> innerResult = Mock()

		PreparedStatement statement = Mock()

		when:
		Result<?> result = request.execute(context)

		then:
		1 * request.select(request.type, _) >> innerSelect
		// FIXME? Cannot mock final
		1 * innerSelect.executeThrowing(context) >> innerResult
		1 * innerResult.getIds() >> existent.collect { it.key }
		1 * context.generateStatement({ it.contains("INSERT INTO ${request.name}") }) >> statement
		remaining.each { record ->
			columns.eachWithIndex { column, i ->
				1 * column.contribute(statement, record, i, context)
			}
		}
		remaining.size() * statement.addBatch()
		1 * statement.executeBatch()
		result.getRecords() == remaining as Set
	}
}
