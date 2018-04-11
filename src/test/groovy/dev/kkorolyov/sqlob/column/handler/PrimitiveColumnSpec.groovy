package dev.kkorolyov.sqlob.column.handler

import dev.kkorolyov.simplefuncs.function.ThrowingBiFunction
import dev.kkorolyov.sqlob.column.FieldBackedColumn
import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.util.Where

import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.ResultSet
import java.sql.SQLException

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class PrimitiveColumnSpec extends Specification {
	class Stub {
		String value
	}
	Field f = Stub.getDeclaredField("value")
	String name = f.getName()
	ThrowingBiFunction<ResultSet, String, ?, SQLException> extractor = new ThrowingBiFunction<ResultSet, String, Object, SQLException>() {
		@Override
		Object applyThrowing(ResultSet resultSet, String s) throws SQLException {
			return resultSet.getObject(s)
		}
	}
	ExecutionContext context = Mock()

	PrimitiveColumnHandler columnFactory = new PrimitiveColumnHandler()
	FieldBackedColumn<?> column = columnFactory.get(f)

	def "contributes same value to where"() {
		Where where = Mock()
		ExecutionContext context = Mock()

		when:
		column.contributeToWhere(where, context)

		then:
		1 * where.resolve(name, { value -> value })
	}

	def "gets field value from instance"() {
		Stub instance = new Stub().with {
			value = randString()
			return it
		}

		expect:
		column.getValue(instance, context) == instance.value
	}
	def "gets value from result set using extractor"() {
		ResultSet rs = Mock()
		String value = randString()

		when:
		String result = column.getValue(rs, context)

		then:
		1 * rs.getString(name) >> value
		result == value
	}
}
