package dev.kkorolyov.sqlob.column

import dev.kkorolyov.sqlob.request.ExecutionContext
import dev.kkorolyov.sqlob.util.Where

import spock.lang.Shared

import java.sql.ResultSet

class ColumnSpec extends BaseColumnSpec {
	@Shared String sqlType = UUID.randomUUID().toString()

	@Override
	Column<?> getTestTarget() {
		return Spy(Column, constructorArgs: [f, sqlType, extractor])
	}

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
			f = GroovyMock()
			return it
		}

		expect:
		column.getValue(instance, context) == instance.f
	}
	def "gets value from result set using extractor"() {
		ResultSet rs = Mock()
		Object value = Mock()

		when:
		Object result = column.getValue(rs, context)

		then:
		1 * rs.getObject(name) >> value
		result == value
	}
}
