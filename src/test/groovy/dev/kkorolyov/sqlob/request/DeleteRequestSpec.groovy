package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.Stub
import dev.kkorolyov.sqlob.util.Where

import java.sql.PreparedStatement

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class DeleteRequestSpec extends BaseRequestSpec<DeleteRequest<?>> {
		Where where = Where.eq(randString(), randString()) // Yay for non-initialized fields at this point

		DeleteRequest<?> request = new DeleteRequest(Stub.BasicStub, randString(), where, columns)

	def "deletes by where clause"() {
		PreparedStatement statement = Mock()

		when:
		request.execute(context)

		then:
		1 * context.generateStatement("DELETE FROM ${request.name} WHERE ${where.sql}") >> statement
		columns.each { 1 * it.contribute(statement, where, context) >> statement }
		1 * statement.executeUpdate()
	}
}
