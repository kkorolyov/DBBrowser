package dev.kkorolyov.sqlob.column.handler

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.column.FieldBackedColumn
import dev.kkorolyov.sqlob.request.CreateRequest

import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.stream.Collectors

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class ReferencingColumnHandlerSpec extends Specification {
	class Stub {
		Stub1 value = new Stub1()
		String otherValue = randString()
	}
	class Stub1 {
		String value = randString()
	}
	Stub instance = new Stub()
	Field f = Stub.getDeclaredField("value")
	Field otherF = Stub.getDeclaredField("otherValue")

	PreparedStatement statement = Mock()
	ResultSet rs = Mock()
	DatabaseMetaData metaData = Mock()
	String database = "SQLite"
	ExecutionContext context = Mock()

	ReferencingColumnHandler handler = new ReferencingColumnHandler()
	FieldBackedColumn<?> column = handler.get(f)

	def "accepts field not accepted by other handlers"() {
		// TODO Write in a way that works
//		expect:
//		handler.accepts(f)
	}
	def "rejects field accepted by primitive handler"() {
		expect:
		!handler.accepts(otherF)
	}

	def "expands creates to entire referenced type hierarchy"() {
		expect:
		handler.expandCreates(
				new CreateRequest<Stub>(Stub)).collect(Collectors.toSet()) ==
				[Stub, Stub1].collect { new CreateRequest<>(it) } as Set
	}

	def "column resolves null criterion to null"() {
		expect:
		column.resolveCriterion(null, context) == null
	}
	def "column resolves criterion to select result's ID"() {
		when:
		Object result = column.resolveCriterion(instance.value, context)

		then:
		1 * context.getMetadata() >> metaData
		1 * context.prepareStatement({ it -> it.contains("SELECT") }) >> statement
		1 * statement.executeQuery() >> rs
		1 * rs.next() >> false
		result instanceof UUID
	}

	def "column gets ID of persisted field value record"() {
		when:
		column.getValue(instance, context)

		then:
		3 * context.getMetadata() >> metaData
		3 * metaData.getDatabaseProductName() >> database
		1 * context.prepareStatement({ it -> it.contains("SELECT") }) >> statement
		1 * statement.executeQuery() >> rs
		1 * rs.next() >> false
		1 * context.prepareStatement({ it -> it.contains("INSERT") }) >> statement
		1 * statement.setObject(1, _ as UUID)
	}
	def "column gets selected object from result set column ID"() {
		UUID valueId = UUID.randomUUID()

		when:
		column.getValue(rs, context)

		then:
		2 * context.getMetadata() >> metaData
		2 * metaData.getDatabaseProductName() >> database
		1 * context.prepareStatement({ it -> it.contains("SELECT") }) >> statement
		1 * statement.executeQuery() >> rs
		1 * rs.getString(f.getName()) >> valueId.toString()
		1 * statement.setObject(1, valueId)
	}
}
