package dev.kkorolyov.sqlob.request

import spock.lang.Specification

import java.sql.Connection
import java.sql.PreparedStatement

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
	Connection connection = Mock()
	ExecutionContext context = Mock() {
		getConnection() >> connection
	}

	InsertRequest<?> request = new InsertRequest(records)

	def "inserts ID of each record"() {
		when:
		request.executeInContext(context)

		then:
		1 * connection.prepareStatement({ it.contains("INSERT INTO ${type.getSimpleName()}") }) >> statement
		1 * statement.setObject(1, id.toString())
		1 * statement.setObject(1, id1.toString())
	}
}
