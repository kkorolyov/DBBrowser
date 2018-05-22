package dev.kkorolyov.sqlob.column

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.result.ConfigurableRecord
import dev.kkorolyov.sqlob.result.Record
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
	Record<UUID, Stub> record = new ConfigurableRecord<>(UUID.randomUUID(), instance)

	String name = "value"
	SqlobType sqlobType = Mock()
	Field f = Stub.getDeclaredField(name)

	ExecutionContext context = Mock()
	DatabaseMetaData metaData = Mock()

	FieldBackedColumn<String> column = new FieldBackedColumn(f, sqlobType)

	def "contributes instance field value to statement"() {
		PreparedStatement statement = Mock()
		int index = randInt()

		when:
		column.contribute(statement, record, index, context)

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.set(metaData, statement, index, instance.value)
	}
	def "contributes result set value to instance field"() {
		ResultSet rs = Mock()
		String newValue = randString()

		when:
		column.contribute(record, rs, context)

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.get(metaData, rs, name) >> newValue
		instance.value == newValue
	}
}
