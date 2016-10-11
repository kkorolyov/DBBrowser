package dev.kkorolyov.sqlob.construct.statement;

import java.util.List;

import dev.kkorolyov.sqlob.connection.DatabaseConnection;
import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Entry;

/**
 * Produces {@code StatementCommands} for execution by a single {@code DatabaseConnection}.
 * @see StatementCommand
 * @see DatabaseConnection
 */
public class StatementFactory {
	private final DatabaseConnection conn;

	/**
	 * Constructs a new statement factory for a database connection.
	 * @param conn connection to produce statements for
	 */
	public StatementFactory(DatabaseConnection conn) {
		this.conn = conn;
	}
	
	/**
	 * Produces a custom query statement.
	 * @param baseStatement base SQL statement, with {@code ?} denoting areas of substitution with parameters
	 * @param parameters parameters to utilize in statement, will be substituted into the base statement in order of declaration
	 */
	public QueryStatement getCustomQuery(String baseStatement, List<Entry> parameters) {
		return new QueryStatement(baseStatement, parameters, null, conn);
	}
	
	/**
	 * Produces a {@code SELECT} statement.
	 * @param table table to select from
	 * @param columns columns to select
	 * @param criteria selection criteria, if {@code null} or empty, no criteria is used
	 */
	public QueryStatement getSelect(String table, List<Column> columns, List<Entry> criteria) {
		return new QueryStatement(StatementBuilder.buildSelect(table, columns, criteria), null, criteria, conn);
	}
	
	/**
	 * Produces a custom update statement.
	 * @param baseStatement base SQL statement, with {@code ?} denoting areas of substitution with parameters
	 * @param parameters parameters to utilize in statement, will be substituted into the base statement in order of declaration
	 */
	public UpdateStatement getCustomUpdate(String baseStatement, List<Entry> parameters) {
		return new UpdateStatement(baseStatement, parameters, null, conn);
	}
	
	/**
	 * Produces a {@code CREATE TABLE} statement.
	 * @param table new table name
	 * @param columns new table columns
	 */
	public UpdateStatement getCreateTable(String table, List<Column> columns) {
		return new UpdateStatement(StatementBuilder.buildCreate(table, columns), null, null, conn) {
			@Override
			public UpdateStatement getInverseStatement() {
				return getDropTable(table);
			}
		};
	}
	/**
	 * Produces a {@code DROP TABLE} statement.
	 * @param table name of table to drop
	 */
	public UpdateStatement getDropTable(String table) {
		return new UpdateStatement(StatementBuilder.buildDrop(table), null, null, conn) {
			List<Column> columns = getConn().connect(table).getColumns();
			
			@Override
			public UpdateStatement getInverseStatement() {
				return getCreateTable(table, columns);
			}
		};
	}
	
	/**
	 * Produces a {@code INSERT} statement.
	 * @param table table to insert into
	 * @param values values to insert
	 */
	public UpdateStatement getInsert(String table, List<Entry> values) {
		return new UpdateStatement(StatementBuilder.buildInsert(table, values), values, null, conn) {
			@Override
			public UpdateStatement getInverseStatement() {
				return getDelete(table, getValues());
			}
		};
	}
	/**
	 * Produces a {@code DELETE} statement.
	 * @param table table to delete from
	 * @param criteria criteria to match when deleting rows
	 */
	public UpdateStatement getDelete(String table, List<Entry> criteria) {
		return new UpdateStatement(StatementBuilder.buildDelete(table, criteria), null, criteria, conn) {
			@Override
			public UpdateStatement getInverseStatement() {
				return getInsert(table, getCriteria());
			}
		};
	}
	/**
	 * Produces a {@code UPDATE} statement.
	 * @param table table to update
	 * @param values new values to set
	 * @param criteria criteria to match
	 */
	public UpdateStatement getUpdate(String table, List<Entry> values, List<Entry> criteria) {
		return new UpdateStatement(StatementBuilder.buildUpdate(table, values, criteria), values, criteria, conn) {
			@Override
			public UpdateStatement getInverseStatement() {
				return getUpdate(table, getCriteria(), getValues());
			}
		};
	}
}
