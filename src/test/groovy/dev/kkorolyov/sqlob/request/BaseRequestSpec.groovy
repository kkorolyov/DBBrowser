package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.column.Column
import dev.kkorolyov.sqlob.column.handler.ColumnHandler
import dev.kkorolyov.sqlob.column.handler.factory.ColumnHandlerFactory
import dev.kkorolyov.sqlob.contributor.RecordStatementContributor
import dev.kkorolyov.sqlob.contributor.ResultInstanceContributor
import dev.kkorolyov.sqlob.contributor.WhereStatementContributor
import dev.kkorolyov.sqlob.type.SqlobType

import spock.lang.Specification

import static dev.kkorolyov.simplespecs.SpecUtilities.setField

abstract class BaseRequestSpec<T extends Request<?>> extends Specification {
	ColumnHandler columnHandler = Mock()
	Collection<AllContributorColumn> columns = (0..5).collect { Mock(AllContributorColumn) }

	ExecutionContext context = Mock()
	T request = buildRequest()

	abstract T buildRequest()

	def setup() {
		setField("COLUMN_FACTORIES", ColumnHandlerFactory, [columnHandler])
		setField("columns", Request, request, columns.withIndex().collectEntries { column, i -> [(i): column] })
	}

	abstract class AllContributorColumn<T> extends Column<T> implements WhereStatementContributor, RecordStatementContributor, ResultInstanceContributor {
		AllContributorColumn(String name, SqlobType<T> sqlobType) {
			super(name, sqlobType)
		}
	}
}
