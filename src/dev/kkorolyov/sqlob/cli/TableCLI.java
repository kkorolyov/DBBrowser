package dev.kkorolyov.sqlob.cli;

import dev.kkorolyov.sqlob.connection.TableConnection;

/**
 * Command line interface for a table.
 */
public class TableCLI {
	private TableConnection tableConn;
	
	/**
	 * Launches the command line interface for a table
	 * @param tableConn table
	 */
	public TableCLI(TableConnection tableConn) {
		if (tableConn == null)
			throw new IllegalArgumentException("Null table connection");
		
		this.tableConn = tableConn;
	}
}
