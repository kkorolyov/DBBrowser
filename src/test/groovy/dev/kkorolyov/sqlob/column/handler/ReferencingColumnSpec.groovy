package dev.kkorolyov.sqlob.column.handler

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.column.FieldBackedColumn
import dev.kkorolyov.sqlob.util.Where

import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.PreparedStatement
import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class ReferencingColumnSpec extends Specification {
	class Stub {
		String value = randString()
	}
	Stub instance = new Stub()
	Field f = Stub.getDeclaredField("value")

	PreparedStatement statement = Mock()
	ResultSet rs = Mock()
	ExecutionContext context = Mock()

	ReferencingColumnHandler columnFactory = new ReferencingColumnHandler()
	FieldBackedColumn<?> column = columnFactory.get(f)

	def "contributes null to null resolution in where"() {
		Where where = new Where(f.getName(), randString(), null)

		when:
		column.contributeToWhere(where, context)
		where.contributeToStatement(statement)

		then:
		1 * statement.setObject(1, null)
	}
	def "contributes select result to where"() {
		Where where = new Where(f.getName(), randString(), new Stub())

		when:
		column.contributeToWhere(where, context)
		where.contributeToStatement(statement)

		then:
		1 * context.prepareStatement({ it -> it.contains("SELECT") }) >> statement
		1 * statement.executeQuery() >> rs
		1 * rs.next() >> false
		1 * statement.setObject(1, _ as UUID)
	}

	def "gets ID of persisted field value record"() {
		when:
		column.getValue(instance, context)

		then:
		1 * context.prepareStatement({ it -> it.contains("SELECT") }) >> statement
		1 * statement.executeQuery() >> rs
		1 * rs.next() >> false
		1 * context.prepareStatement({ it -> it.contains("INSERT") }) >> statement
		1 * statement.setObject(1, _ as UUID)
	}
	def "gets selected object from result set column ID"() {
		when:
		column.getValue(rs, context)

		then:
		1 * context.prepareStatement({ it -> it.contains("SELECT") }) >> statement
		1 * statement.executeQuery() >> rs
		1 * rs.next() >> false
		1 * statement.setObject(1, null)
	}
}
