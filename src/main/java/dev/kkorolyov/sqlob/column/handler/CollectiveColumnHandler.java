package dev.kkorolyov.sqlob.column.handler;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.request.collection.CreateCollectionRequest;
import dev.kkorolyov.sqlob.request.CreateRequest;
import dev.kkorolyov.sqlob.result.ConfigurableRecord;
import dev.kkorolyov.sqlob.result.Record;
import dev.kkorolyov.sqlob.util.PersistenceHelper;
import dev.kkorolyov.sqlob.util.Where;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Handles collection fields.
 */
public class CollectiveColumnHandler implements ColumnHandler {
	CreateCollectionRequest create(Field f) {
		return new CreateCollectionRequest(f);
	}

	@Override
	public FieldBackedColumn<?> get(Field f) {
		return new CollectiveColumn(f);
	}
	@Override
	public boolean accepts(Field f) {
		return Collection.class.isAssignableFrom(f.getType());
	}

	@Override
	public Stream<CreateRequest<?>> expandCreates(CreateRequest<?> primaryRequest) {
		return Stream.of(primaryRequest);
	}

	private class CollectiveColumn extends FieldBackedColumn<Collection<?>> {
		private final KeyColumn keyDelegate;

		CollectiveColumn(Field f) {
			// Relies on delegate
			super(f, null);
			this.keyDelegate = KeyColumn.foreign(PersistenceHelper.getName(f), PersistenceHelper.getName(f.getType()));
		}

		@Override
		public PreparedStatement contribute(PreparedStatement statement, Where where, ExecutionContext context) {
			create(getField());

			where.consumeValues(getName(), (index, value) ->
				);

			return statement;
		}
		@Override
		public <O> PreparedStatement contribute(PreparedStatement statement, Record<UUID, O> instance, int index, ExecutionContext context) {
			create(getField());

			// TODO Execute InsertCollection request instead
			return super.contribute(statement, instance, index, context);
		}
		@Override
		public <O> ConfigurableRecord<UUID, O> contribute(ConfigurableRecord<UUID, O> record, ResultSet rs, ExecutionContext context) {
			create(getField());

			// TODO Execute SelectCollection request instead
			return super.contribute(record, rs, context);
		}

		@Override
		public Collection<?> getValue(ResultSet rs, ExecutionContext context) {
			return super.getValue(rs, context);
		}

		@Override
		public String getSql(ExecutionContext context) {
			return keyDelegate.getSql(context);
		}
	}
}
