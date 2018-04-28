package dev.kkorolyov.sqlob.column

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.type.SqlobType

import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.DatabaseMetaData
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
	SqlobType sqlobType = Mock()
	String name = "value"

	PreparedStatement statement = Mock()
	ResultSet rs = Mock()
	DatabaseMetaData metaData = Mock()
	ExecutionContext context = Mock()

	FieldBackedColumn<String> column = new FieldBackedColumn(f, sqlobType)

	def "contributes instance field value to statement"() {
		int index = randInt()

		when:
		column.contributeToStatement(statement, instance, index, context)

		then:
		1 * context.getMetadata() >> metaData
		1 * sqlobType.set(metaData, statement, index, instance.value)
	}
	def "contributes result set value to instance field"() {
		String newValue = randString()

		when:
		column.contributeToInstance(instance, rs, context)

		then:
		1 * context.getMetadata() >> metaData
		1 * sqlobType.get(metaData, rs, name) >> newValue
		instance.value == newValue
	}

	def "gets instance field value"() {
		expect:
		column.getValue(instance, context) == instance.value
	}
}
