package dev.kkorolyov.sqlob.column

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.type.SqlobType

import spock.lang.Specification

import java.sql.DatabaseMetaData
import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class ColumnSpec extends Specification {
	String name = randString()
	SqlobType<String> sqlobType = Mock()

	ExecutionContext context = Mock()
	DatabaseMetaData metaData

	Column<String> column = new Column<>(name, sqlobType)

	def "resolves criterion by casting to column type"() {
		Object value = randString()

		expect:
		column.resolveCriterion(value, context).is(value)
	}

	def "gets value from result set using sqlob type"() {
		ResultSet rs = Mock()
		String expectedValue = randString()

		when:
		String value = column.getValue(rs, context)

		then:
		1 * context.getMetadata() >> metaData
		1 * sqlobType.get(metaData, rs, name) >> expectedValue
		value == expectedValue
	}

	def "sql is name and sqlob type"() {
		String expectedSqlTypo = randString()

		when:
		String sqlType = column.getSql(context)

		then:
		1 * context.getMetadata() >> metaData
		1 * sqlobType.getSqlType(metaData) >> expectedSqlTypo
		sqlType == "$name $expectedSqlTypo"
	}
}
