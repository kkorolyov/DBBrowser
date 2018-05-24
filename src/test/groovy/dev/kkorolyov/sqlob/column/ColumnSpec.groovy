package dev.kkorolyov.sqlob.column

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.type.SqlobType
import dev.kkorolyov.sqlob.util.Where

import spock.lang.Specification

import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class ColumnSpec extends Specification {
	String name = randString()
	SqlobType<String> sqlobType = Mock()

	ExecutionContext context = Mock()
	DatabaseMetaData metaData = Mock()

	Column<String> column = new Column<>(name, sqlobType)

	def "contributes criterion directly"() {
		Object value = randString()
		Where where = Where.eq(name, value);
		PreparedStatement statement = Mock()

		when:
		column.contribute(statement, where, context);

		then:
		1 * context.metadata >> metaData
		sqlobType.set(metaData, statement, 0, value)
	}

	def "gets value from result set using sqlob type"() {
		ResultSet rs = Mock()
		String expectedValue = randString()

		when:
		String value = column.getValue(rs, context)

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.get(metaData, rs, name) >> expectedValue
		value == expectedValue
	}

	def "sql is name and sqlob type"() {
		String expectedSqlTypo = randString()

		when:
		String sqlType = column.getSql(context)

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.getSqlType(metaData) >> expectedSqlTypo
		sqlType == "$name $expectedSqlTypo"
	}
}
