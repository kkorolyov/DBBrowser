package dev.kkorolyov.sqlob.column

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.result.ConfigurableRecord
import dev.kkorolyov.sqlob.type.SqlobType

import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.DatabaseMetaData
import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class FieldBackedColumnSpec extends Specification {
	Stub instance = new Stub()
	ConfigurableRecord<UUID, Stub> record = new ConfigurableRecord<>(UUID.randomUUID(), instance)

	String name = "value"
	SqlobType sqlobType = Mock()
	Field f = Stub.getDeclaredField(name)

	ExecutionContext context = Mock()
	DatabaseMetaData metaData = Mock()

	FieldBackedColumn<String> column = new FieldBackedColumn(f, sqlobType)

	def "uses non-annotated field name as name"() {
		expect:
		new FieldBackedColumn(Stub.getDeclaredField("value"), sqlobType).name == "value"
	}
	def "uses annotation's value as name"() {
		expect:
		new FieldBackedColumn(Stub.getDeclaredField("annotated"), sqlobType).name == "custom"
	}

	def "gets record's object's field value"() {
		when:
		Object result = column.get(record, context)

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.get(metaData, instance.value) >> instance.value
		result == instance.value
	}
	def "sets resultSet value on record's object's field"() {
		ResultSet rs = Mock()
		String newValue = randString()

		when:
		column.set(record, rs, context)

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.get(metaData, rs, name) >> newValue
		instance.value == newValue
	}

	class Stub {
		String value = randString()
		@dev.kkorolyov.sqlob.annotation.Column("custom")
		String annotated = randString()
	}
}
