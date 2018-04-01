package dev.kkorolyov.sqlob.column

import dev.kkorolyov.sqlob.ExecutionContext

import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.PreparedStatement
import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.randInt
import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class FieldBackedColumnSpec extends Specification {
	class Stub {
		String value = randString()
	}
	Stub instance = new Stub()
	Field f = Stub.getDeclaredField("value")
	String sqlType = randString()

	PreparedStatement statement = Mock()
	ResultSet rs = Mock()
	ExecutionContext context = Mock()

	FieldBackedColumn<String> column = Spy(constructorArgs: [f, sqlType])

	def "contributes instance value to statement"() {
		int index = randInt()

		when:
		column.contributeToStatement(statement, instance, index, context)

		then:
		1 * statement.setObject(index, instance.value)
	}
	def "contributes statement value to instance field"() {
		String newValue = randString()

		when:
		column.contributeToInstance(instance, rs, context)

		then:
		1 * column.getValue(rs, context) >> newValue
		instance.value == newValue
	}

	def "gets instance field value"() {
		expect:
		column.getValue(instance, context) == instance.value
	}
}
