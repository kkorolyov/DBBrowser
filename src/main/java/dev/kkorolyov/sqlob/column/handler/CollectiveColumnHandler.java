package dev.kkorolyov.sqlob.column.handler;

import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.request.CreateRequest;
import dev.kkorolyov.sqlob.util.PersistenceHelper;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Handles collection fields.
 */
public class CollectiveColumnHandler implements ColumnHandler {
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

	private static class CollectiveColumn extends FieldBackedColumn<Collection> {
		private final KeyColumn keyDelegate;

		CollectiveColumn(Field f) {
			this(f, KeyColumn.foreign(PersistenceHelper.getName(f), PersistenceHelper.getName(f.getType())));
		}
		private CollectiveColumn(Field f, KeyColumn keyDelegate) {
			super(f, null);
			this.keyDelegate = keyDelegate;
		}

		@Override
		public PreparedStatement contributeToStatement(PreparedStatement statement, Object instance, int index, ExecutionContext context) {
			// TODO Execute InsertCollection request instead
			return super.contributeToStatement(statement, instance, index, context);
		}
		@Override
		public Object contributeToInstance(Object instance, ResultSet rs, ExecutionContext context) {
			// TODO Execute SelectCollection request instead
			return super.contributeToInstance(instance, rs, context);
		}

		@Override
		public Collection resolveCriterion(Object value, ExecutionContext context) {
			return super.resolveCriterion(value, context);
		}

		@Override
		public Collection getValue(Object instance, ExecutionContext context) {
			return super.getValue(instance, context);
		}
		@Override
		public Collection getValue(ResultSet rs, ExecutionContext context) {
			return super.getValue(rs, context);
		}

		@Override
		public String getSql(ExecutionContext context) {
			return keyDelegate.getSql(context);
		}
	}
}
