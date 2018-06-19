package dev.kkorolyov.sqlob.request

import dev.kkorolyov.simplefiles.Providers
import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.column.Column
import dev.kkorolyov.sqlob.column.handler.ColumnHandler
import dev.kkorolyov.sqlob.column.handler.factory.ColumnHandlerFactory
import dev.kkorolyov.sqlob.result.ConfigurableRecord
import dev.kkorolyov.sqlob.result.Record
import dev.kkorolyov.sqlob.struct.Table

import spock.lang.Specification

import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.getField
import static dev.kkorolyov.simplespecs.SpecUtilities.randString
import static dev.kkorolyov.simplespecs.SpecUtilities.setField

abstract class BaseRequestSpec<T extends Request<?>> extends Specification {
	static final Providers<ColumnHandler> ORIGINAL_COLUMN_HANDLERS = getField("COLUMN_HANDLERS", ColumnHandlerFactory)

	ColumnHandler columnHandler = Mock(ColumnHandler) {
		accepts(_) >> true
	}.with {
		setField("COLUMN_HANDLERS", ColumnHandlerFactory, Providers.fromInstances(ColumnHandler, [it] as Set))
		return it
	}
	Collection<Column<?>> columns = (0..5).collect {Spy(MockColumn)}
	ExecutionContext context = Mock()

	def cleanupSpec() {
		setField("COLUMN_HANDLERS", ColumnHandlerFactory, ORIGINAL_COLUMN_HANDLERS)
	}

	class MockColumn<T> extends Column<T> {
		MockColumn() {
			super(randString(), null)
		}

		@Override
		Object get(Record<UUID, ?> record, ExecutionContext context) {
			return null
		}
		@Override
		<O> ConfigurableRecord<UUID, O> set(ConfigurableRecord<UUID, O> record, ResultSet rs, ExecutionContext context) {
			return null
		}

		@Override
		String getSql(ExecutionContext context) {
			return ""
		}

		@Override
		Collection<Table> getPrerequisites(ExecutionContext context) {
			return Collections.emptySet()
		}
	}
}
