package dev.kkorolyov.sqlob.connection;

import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Results;
import dev.kkorolyov.sqlob.statement.ResultingStatement;
import dev.kkorolyov.sqlob.statement.UpdatingStatement;

/**
 * A stub {@code DatabaseConnection} for testing purposes.
 */
public class DatabaseConnectionStub implements DatabaseConnection {

	@Override
	public TableConnection connect(String table) {
		return new TableConnection(this, table);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Results execute(ResultingStatement statement) {
		/* TODO Auto-generated method stub */
		return null;
	}
	@Override
	public int execute(UpdatingStatement statement) {
		/* TODO Auto-generated method stub */
		return 0;
	}

	@Override
	public Results runStatement(ResultingStatement statement) {
		/* TODO Auto-generated method stub */
		return null;
	}
	@Override
	public int runStatement(UpdatingStatement statement) {
		/* TODO Auto-generated method stub */
		return 0;
	}

	@Override
	public int revertLastStatement() {
		/* TODO Auto-generated method stub */
		return 0;
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public TableConnection createTable(String name, Column[] columns)
			throws DuplicateTableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean dropTable(String table) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsTable(String table) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String[] getTables() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDatabaseName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addStatementListener(StatementListener listener) {
		/* TODO Auto-generated method stub */
		
	}
	@Override
	public void removeStatementListener(StatementListener listener) {
		/* TODO Auto-generated method stub */
		
	}
}
