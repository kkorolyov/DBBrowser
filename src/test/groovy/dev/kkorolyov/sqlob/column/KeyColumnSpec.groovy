package dev.kkorolyov.sqlob.column

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.util.Where

import spock.lang.Specification

import java.sql.PreparedStatement
import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class KeyColumnSpec extends Specification {
	String attribute = randString()
	UUID key = UUID.randomUUID()
	Where where = new Where(attribute, randString(), key)

	ExecutionContext context = Mock()
	PreparedStatement statement = Mock()
	ResultSet rs = Mock()

	KeyColumn column = KeyColumn.primary(attribute)

	def "contributes same value to where"() {
		when:
		column.contributeToWhere(where, context)
		where.contributeToStatement(statement)

		then:
		1 * statement.setObject(1, key)
	}

	def "gets UUID from result set if non-null"() {
		when:
		UUID result = column.getValue(rs, context)

		then:
		1 * rs.getString(attribute) >> key.toString()
		result == key
	}
	def "gets null from result set if null"() {
		when:
		UUID result = column.getValue(rs, context)

		then:
		1 * rs.getString(attribute) >> null
		result == null
	}
}
