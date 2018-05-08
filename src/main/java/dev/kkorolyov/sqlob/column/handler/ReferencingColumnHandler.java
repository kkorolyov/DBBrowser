package dev.kkorolyov.sqlob.column.handler;

import dev.kkorolyov.simplegraphs.Graph;
import dev.kkorolyov.sqlob.ExecutionContext;
import dev.kkorolyov.sqlob.column.FieldBackedColumn;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.column.handler.factory.ColumnHandlerFactory;
import dev.kkorolyov.sqlob.request.CreateRequest;
import dev.kkorolyov.sqlob.request.InsertRequest;
import dev.kkorolyov.sqlob.request.SelectRequest;
import dev.kkorolyov.sqlob.type.SqlobType;
import dev.kkorolyov.sqlob.util.PersistenceHelper;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Handles fields mapped to foreign-key columns.
 * Accepts all types not accepted by any other column handler.
 */
public class ReferencingColumnHandler implements ColumnHandler {
	@Override
	public FieldBackedColumn<?> get(Field f) {
		return new ReferencingColumn(f);
	}
	@Override
	public boolean accepts(Field f) {
		return ColumnHandlerFactory.stream()
				.filter(columnHandler -> !columnHandler.equals(this))
				.noneMatch(columnHandler -> columnHandler.accepts(f));
	}

	@Override
	public Stream<CreateRequest<?>> expandCreates(CreateRequest<?> primaryRequest) {
		Graph<Class<?>> typeDependencies = new Graph<>();
		Map<Class<?>, CreateRequest<?>> requests = new HashMap<>();
		Queue<CreateRequest<?>> requestQueue = new ArrayDeque<>();

		for (CreateRequest<?> request = primaryRequest; request != null; request = requestQueue.poll()) {
			Class<?> requestType = request.getType();
			requests.put(requestType, request);

			request.streamColumns(ReferencingColumn.class)
					.map(ReferencingColumn::getType)
					.peek(referencedType -> typeDependencies.add(referencedType, requestType))
					.filter(referencedType -> !requests.containsKey(referencedType))
					.map(CreateRequest::new)
					.forEach(requestQueue::add);
		}
		return typeDependencies.sortTopological().stream()
				.map(requests::get);
	}

	private static class ReferencingColumn extends FieldBackedColumn<Object> {
		private final KeyColumn keyDelegate;

		ReferencingColumn(Field f) {
			this(f, KeyColumn.foreign(PersistenceHelper.getName(f), PersistenceHelper.getName(f.getType())));
		}
		private ReferencingColumn(Field f, KeyColumn keyDelegate) {
			super(f, (SqlobType<Object>) (SqlobType<?>) keyDelegate.getSqlobType());
			this.keyDelegate = keyDelegate;
		}

		@Override
		public Object resolveCriterion(Object value, ExecutionContext context) {
			return value != null
					? new SelectRequest<>(value)
					.execute(context)
					.getId().orElseGet(UUID::randomUUID)
					: null;
		}
		@Override
		public Object getValue(Object instance, ExecutionContext context) {
			return new InsertRequest<>(super.getValue(instance, context))
					.execute(context)
					.getId()
					.orElseThrow(() -> new IllegalStateException("This should never happen"));
		}
		@Override
		public Object getValue(ResultSet rs, ExecutionContext context) {
			return new SelectRequest<>(getType(), keyDelegate.getValue(rs, context))
					.execute(context)
					.getObject().orElse(null);
		}
	}
}
