package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.ExecutionContext

import spock.lang.Specification

import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.ResultSet

class InsertRequestSpec extends Specification {
	class Stub {
		String value
	}
	UUID id = UUID.randomUUID()
	UUID id1 = UUID.randomUUID()
	Stub obj = new Stub()
	Stub obj1 = new Stub()
	Map<UUID, ?> records = [
			(id): obj,
			(id1): obj1,
	]

	String database = "SQLite"
	DatabaseMetaData metaData = Mock()
	PreparedStatement statement = Mock()
	ResultSet rs = Mock()
	ExecutionContext context = Mock()

	InsertRequest<?> request = new InsertRequest(records)

	def "inserts ID of each record"() {
		when:
		request.execute(context)

		then:
		6 * metaData.getDatabaseProductName() >> database
		6 * context.getMetadata() >> metaData
		1 * context.prepareStatement({ it.contains("SELECT") && it.contains("FROM ${Stub.getSimpleName()}") }) >> statement
		1 * statement.executeQuery() >> rs
		1 * rs.next() >> false
		1 * context.prepareStatement({ it.contains("INSERT INTO ${Stub.getSimpleName()}") }) >> statement
		1 * statement.setObject(1, id)
		1 * statement.setObject(1, id1)
		1 * statement.executeBatch()
	}
}
