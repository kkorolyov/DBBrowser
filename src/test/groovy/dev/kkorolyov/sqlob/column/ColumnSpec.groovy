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
	DatabaseMetaData metaData = Mock()

	Column<String> column = Spy(Column, constructorArgs: [name, sqlobType])

	def "resolves value using sqlob type"() {
		String expectedValue = randString()

		when:
		String value = column.resolve(expectedValue, context)

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.get(metaData, expectedValue) >> expectedValue
		value == expectedValue
	}

	def "gets value from result set using sqlob type"() {
		ResultSet rs = Mock()
		String expectedValue = randString()

		when:
		String value = column.get(rs, context)

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.get(metaData, rs, name) >> expectedValue
		value == expectedValue
	}

	def "sql is name and sqlob type"() {
		String expectedSqlType = randString()

		when:
		String sqlType = column.getSql(context)

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.getSqlType(metaData) >> expectedSqlType
		sqlType == "$name $expectedSqlType"
	}

	def "has no prerequisites"() {
		expect:
		column.getPrerequisites(context) == [] as Set
	}
}
