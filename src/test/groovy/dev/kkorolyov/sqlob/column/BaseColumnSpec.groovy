package dev.kkorolyov.sqlob.column

import dev.kkorolyov.simplefuncs.function.ThrowingBiFunction
import dev.kkorolyov.sqlob.request.ExecutionContext

import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

abstract class BaseColumnSpec extends Specification {
	class Stub {
		Object f
	}
	Field f = Stub.class.getDeclaredField("f")
	String name = f.getName()
	ThrowingBiFunction<ResultSet, String, ?, SQLException> extractor = new ThrowingBiFunction<ResultSet, String, Object, SQLException>() {
		@Override
		Object applyThrowing(ResultSet resultSet, String s) throws SQLException {
			return resultSet.getObject(s)
		}
	}
	ExecutionContext context = Mock()

	Column<?> column = getTestTarget()

	/** @return column under test; should be a spy */
	abstract Column<?> getTestTarget()

	def "contributes value from instance to statement"() {
		PreparedStatement statement = Mock()
		Stub instance = new Stub().with {
			f = GroovyMock()
			return it
		}
		int index = new Random().nextInt()
		Object value = Mock()

		when:
		column.contributeToStatement(statement, instance, index, context)

		then:
		1 * column.getValue(instance, context) >> value
		1 * statement.setObject(index, value)
	}
	def "contributes value from result set to instance"() {
		Stub instance = new Stub()
		ResultSet rs = Mock()
		Object value = Mock()

		when:
		column.contributeToInstance(instance, rs, context)

		then:
		1 * column.getValue(rs, context) >> value
		instance.f == value
	}
}
