package dev.kkorolyov.sqlob.column.handler

import dev.kkorolyov.simplefiles.Providers
import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.column.Column
import dev.kkorolyov.sqlob.column.FieldBackedColumn
import dev.kkorolyov.sqlob.column.KeyColumn
import dev.kkorolyov.sqlob.column.handler.factory.ColumnHandlerFactory
import dev.kkorolyov.sqlob.request.InsertRequest
import dev.kkorolyov.sqlob.request.SelectRequest
import dev.kkorolyov.sqlob.result.ConfigurableRecord
import dev.kkorolyov.sqlob.result.Record
import dev.kkorolyov.sqlob.result.Result
import dev.kkorolyov.sqlob.type.SqlobType

import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.DatabaseMetaData
import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.getField
import static dev.kkorolyov.simplespecs.SpecUtilities.randString
import static dev.kkorolyov.simplespecs.SpecUtilities.setField

class ReferencingColumnHandlerSpec extends Specification {
	static final Providers<ColumnHandler> ORIGINAL_COLUMN_HANDLERS = getField("COLUMN_HANDLERS", ColumnHandlerFactory)

	// HANDLER TESTS
	ColumnHandler otherHandler = Mock()

	ReferencingColumnHandler handler = Spy()

	def cleanupSpec() {
		setField("COLUMN_HANDLERS", ColumnHandlerFactory, ORIGINAL_COLUMN_HANDLERS)
	}

	def setup() {
		setupHandler()
		setupColumn()
	}
	def setupHandler() {
		setField("COLUMN_HANDLERS", ColumnHandlerFactory, Providers.fromInstances(ColumnHandler, [otherHandler, handler] as Set))
	}

	def "rejects field accepted by other handlers"() {
		1 * otherHandler.accepts(_) >> true

		expect:
		!handler.accepts(Stub.getDeclaredField("value"))
	}
	def "accepts field not accepted by other handlers"() {
		1 * otherHandler.accepts(_) >> false

		expect:
		handler.accepts(Stub.getDeclaredField("value"))
	}

	// COLUMN TESTS
	String name = "value"
	SqlobType<UUID> sqlobType = Mock()
	Field f = Stub.getDeclaredField(name)

	Stub instance = new Stub()
	Record<UUID, Stub> record = new ConfigurableRecord<>(UUID.randomUUID(), instance)

	ExecutionContext context = Mock()
	DatabaseMetaData metaData = Mock()

	FieldBackedColumn<?> column = handler.get(f)

	def setupColumn() {
		KeyColumn keyDelegate = getField("keyDelegate", column) as KeyColumn
		setField("sqlobType", Column, keyDelegate, sqlobType)
	}

	def "resolves null criterion to null"() {
		expect:
		column.resolve(null, context) == null
	}
	def "resolves criterion to select result's ID"() {
		Object value = randString()
		UUID id = UUID.randomUUID()

		SelectRequest<?> request = Mock()
		Result<?> result = Mock()

		when:
		def resolved = column.resolve(value, context)

		then:
		1 * handler.select(value) >> request
		// FIXME? Can't mock final execute()
		1 * request.executeThrowing(context) >> result
		1 * result.key >> Optional.of(id)
		resolved == id
	}

	def "gets persisted field value record's ID"() {
		InsertRequest<?> request = Mock()
		Result<?> result = Mock()

		when:
		def value = column.get(record, context)

		then:
		1 * handler.insert(instance.value) >> request
		// FIXME? Can't mock final execute()
		1 * request.executeThrowing(context) >> result
		1 * result.key >> Optional.of(record.key)
		1 * context.getMetadata() >> metaData
		1 * sqlobType.get(metaData, record.key) >> record.key
		value == record.key
	}

	def "gets field value record's object from result set"() {
		UUID id = UUID.randomUUID()

		ResultSet rs = Mock()

		SelectRequest<?> request = Mock()
		Result<?> result = Mock()

		when:
		def value = column.get(rs, context)

		then:
		1 * context.getMetadata() >> metaData
		1 * sqlobType.get(metaData, rs, name) >> id
		1 * handler.select(f.getType(), id) >> request
		// FIXME? Can't mock final execute()
		1 * request.executeThrowing(context) >> result
		1 * result.getObject() >> Optional.of(instance.value)

		value == instance.value
	}

	// TODO Test prerequisites

	class Stub {
		Stub1 value = new Stub1()
	}

	class Stub1 {
		String value = randString()
	}
}
