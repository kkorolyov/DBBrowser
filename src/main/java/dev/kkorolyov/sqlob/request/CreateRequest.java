package dev.kkorolyov.sqlob.request;

import dev.kkorolyov.simplegraphs.Graph;
import dev.kkorolyov.sqlob.column.Column;
import dev.kkorolyov.sqlob.column.KeyColumn;
import dev.kkorolyov.sqlob.column.factory.ReferencingColumnFactory.ReferencingColumn;
import dev.kkorolyov.sqlob.result.ConfigurableResult;
import dev.kkorolyov.sqlob.result.Result;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Request to create a table for a specific class.
 */
public class CreateRequest<T> extends Request<T> {
	/**
	 * Constructs a new create request.
	 * @see Request#Request(Class)
	 */
	public CreateRequest(Class<T> type) {
		super(type);
	}

	@Override
	Result<T> executeInContext(ExecutionContext context) throws SQLException {
		Graph<Class<?>> typeDependencies = new Graph<>();
		typeDependencies.add(getType());
		Map<Class<?>, CreateRequest<?>> prereqRequests = new HashMap<>();
		prereqRequests.put(getType(), this);

		// Add all prereq types to graph, add request for each distinct type
		loadPrereqs(typeDependencies, prereqRequests);

		List<String> sql = typeDependencies.getTopologicalSorting().stream()
				.map(prereqRequests::get)
				.map(CreateRequest::getCreateStatement)
				.collect(Collectors.toList());

		logStatements(sql);

		Statement statement = context.getStatement();
		for (String s : sql) statement.addBatch(s);
		statement.executeBatch();

		return new ConfigurableResult<>();
	}

	private void loadPrereqs(Graph<Class<?>> typeDependencies, Map<Class<?>, CreateRequest<?>> prereqRequests) {
		getColumns(ReferencingColumn.class).stream()
				.map(ReferencingColumn::getType)
				.peek(referencedType -> typeDependencies.add(referencedType, getType()))
				.filter(referencedType -> !prereqRequests.containsKey(referencedType))
				.map(CreateRequest::new)
				.forEach(prereqRequest -> {
					prereqRequests.put(prereqRequest.getType(), prereqRequest);
					prereqRequest.loadPrereqs(typeDependencies, prereqRequests);
				});
	}

	private String getCreateStatement() {
		return getColumns().stream()
				.map(Column::getSql)
				.collect(Collectors.joining(", ",
						"CREATE TABLE IF NOT EXISTS " + getName()
								+ " (" + KeyColumn.PRIMARY.getSql() + ", ",
						")"));
	}
}
