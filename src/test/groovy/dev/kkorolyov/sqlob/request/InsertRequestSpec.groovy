package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.ExecutionContext

import spock.lang.Specification

import java.sql.PreparedStatement
import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class InsertRequestSpec extends Specification {
	Class<?> type = String

	UUID id = UUID.randomUUID()
	UUID id1 = UUID.randomUUID()
	String obj = randString()
	String obj1 = randString()
	Map<UUID, ?> records = [
			(id): obj,
			(id1): obj1,
	]

	PreparedStatement statement = Mock()
	ResultSet rs = Mock()
	ExecutionContext context = Mock()

	InsertRequest<?> request = new InsertRequest(records)

	def "inserts ID of each record"() {
		when:
		request.execute(context)

		then:
		1 * context.prepareStatement({ it.contains("SELECT") && it.contains("FROM ${type.getSimpleName()}") }) >> statement
		1 * statement.executeQuery() >> rs
		1 * rs.next() >> false
		1 * context.prepareStatement({ it.contains("INSERT INTO ${type.getSimpleName()}") }) >> statement
		1 * statement.setObject(1, id)
		1 * statement.setObject(1, id1)
	}
}
