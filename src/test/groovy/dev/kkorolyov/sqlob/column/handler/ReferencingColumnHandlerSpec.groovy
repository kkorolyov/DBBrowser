package dev.kkorolyov.sqlob.column.handler

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.column.Column
import dev.kkorolyov.sqlob.column.FieldBackedColumn
import dev.kkorolyov.sqlob.column.KeyColumn
import dev.kkorolyov.sqlob.request.CreateRequest
import dev.kkorolyov.sqlob.request.InsertRequest
import dev.kkorolyov.sqlob.request.SelectRequest
import dev.kkorolyov.sqlob.result.Record
import dev.kkorolyov.sqlob.result.Result
import dev.kkorolyov.sqlob.type.SqlobType
import dev.kkorolyov.sqlob.util.Where

import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.stream.Collectors

import static dev.kkorolyov.simplespecs.SpecUtilities.*

class ReferencingColumnHandlerSpec extends Specification {
	class Stub {
		Stub1 value = new Stub1()
		String otherValue = randString()
	}
	class Stub1 {
		String value = randString()
	}
	Stub instance = new Stub()
	Record<UUID, Stub> record = new Record<>(UUID.randomUUID(), instance)

	String name = "value"
	SqlobType<UUID> sqlobType = Mock()
	Field f = Stub.getDeclaredField(name)

	ExecutionContext context = Mock()
	DatabaseMetaData metaData = Mock()

	ReferencingColumnHandler handler = Spy()
	FieldBackedColumn<?> column = handler.get(f)

	def setup() {
		KeyColumn keyDelegate = getField("keyDelegate", column) as KeyColumn
		setField("sqlobType", Column, keyDelegate, sqlobType)
	}

	def "accepts field not accepted by other handlers"() {
		// TODO Write in a way that works
//		expect:
//		handler.accepts(f)
	}
	def "rejects field accepted by primitive handler"() {
		expect:
		!handler.accepts(Stub.getDeclaredField("otherValue"))
	}

	def "expands creates to entire referenced type hierarchy"() {
		expect:
		handler.expandCreates(
				new CreateRequest<Stub>(Stub)).collect(Collectors.toSet()) ==
				[Stub, Stub1].collect { new CreateRequest<>(it) } as Set
	}

	def "contributes null criterion as null"() {
		UUID value = null
		Where where = Where.eq(name, value)

		PreparedStatement statement = Mock()

		when:
		column.contribute(statement, where, context);

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.set(metaData, statement, 0, value)
	}
	def "contributes criterion as select result's ID"() {
		Object value = randString()
		UUID id = UUID.randomUUID()
		Where where = Where.eq(name, value)

		PreparedStatement statement = Mock()

		SelectRequest<?> request = Mock()
		Result<?> result = Mock()

		when:
		column.contribute(statement, where, context)

		then:
		1 * context.getMetadata() >> metaData
		1 * handler.select(value) >> request
		// FIXME? Can't mock final execute()
		1 * request.executeThrowing(context) >> result
		1 * result.id >> Optional.of(id)
		1 * sqlobType.set(metaData, statement, 0, id)
	}

	def "contributes persisted field value record's ID to statement"() {
		UUID id = UUID.randomUUID()

		PreparedStatement statement = Mock()
		int index = randInt()

		InsertRequest<?> request = Mock()
		Result<?> result = Mock()

		when:
		column.contribute(statement, new Record<>(id, instance), index, context)

		then:
		1 * context.getMetadata() >> metaData
		1 * handler.insert(instance.value) >> request
		// FIXME? Can't mock final execute()
		1 * request.executeThrowing(context) >> result
		1 * result.id >> Optional.of(id)
		1 * sqlobType.set(metaData, statement, index, id)
	}

	def "gets field value record's object from result set"() {
		UUID id = UUID.randomUUID()

		ResultSet rs = Mock()

		SelectRequest<?> request = Mock()
		Result<?> result = Mock()

		when:
		Object value = column.getValue(rs, context)

		then:
		1 * context.getMetadata() >> metaData
		1 * sqlobType.get(metaData, rs, name) >> id
		1 * handler.select(f.getType(), id) >> request
		// FIXME? Can't mock final execute()
		1 * request.executeThrowing(context) >> result
		1 * result.getObject() >> Optional.of(instance.value)

		value == instance.value
	}
}
