package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.Stub
import dev.kkorolyov.sqlob.util.Where

import java.sql.PreparedStatement

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class DeleteRequestSpec extends BaseRequestSpec<DeleteRequest<?>> {
	Where where

	@Override
	DeleteRequest<?> buildRequest() {
		where = Where.eq(randString(), randString()) // Yay for non-initialized fields at this point

		return new DeleteRequest(Stub.BasicStub, where)
	}

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
