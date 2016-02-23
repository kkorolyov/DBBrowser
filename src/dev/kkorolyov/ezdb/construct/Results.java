package dev.kkorolyov.ezdb.construct;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import dev.kkorolyov.ezdb.exceptions.ClosedException;
import dev.kkorolyov.ezdb.exceptions.MismatchedTypeException;
import dev.kkorolyov.ezdb.logging.DebugLogger;

/**
 * Results obtained from a SQL query.
 * Wraps a {@code ResultSet} object and converts its results to a list of {@code RowEntry[]} objects with values converted to the appropriate Java type.
 * Maintains a cursor initially positioned directly before the first row of results.
 * @see ResultSet
 * @see RowEntry
 */
public class Results implements AutoCloseable {
	private static final DebugLogger log = DebugLogger.getLogger(Results.class.getName());
	
	private ResultSet rs;
	private Column[] columns;
	
	/**
	 * Constructs a new {@code Results} object from a {@code ResultSet}.
	 * @param resultSet result set to wrap
	 */
	public Results(ResultSet resultSet) {
		this.rs = resultSet;
	}
	
	/**
	 * Closes this results object and releases all resources.
	 * Does nothing if called on a closed results.
	 */
	public void close() {
		if (isClosed())
			return;
		
		try {
			rs.close();
			rs = null;
			columns = null;
		} catch (SQLException e) {
			log.exceptionSevere(e);
		}
	}
	/** @return	{@code true} if this resource is closed, {@code false} if otherwise */
	public boolean isClosed() {
		return (rs == null);
	}
	
	/**
	 * Retrieves the column information (name and type) associated with these results.
	 * @return all columns
	 * @throws ClosedException if called on a closed resource
	 */
	public Column[] getColumns() throws ClosedException {
		if (isClosed())
			throw new ClosedException();
		
		if (columns == null) {	// Not yet cached
			try {
				ResultSetMetaData rsmd = rs.getMetaData();
				
				columns = new Column[rsmd.getColumnCount()];
				for (int i = 0; i < columns.length; i++) {
					int rsmdColumn = i + 1;	// ResultSet columns start from 1
					
					String columnName = rsmd.getColumnName(rsmdColumn);
					SqlType columnType = SqlType.get(rsmd.getColumnType(rsmdColumn));
					//System.out.println("Column name: " + columnName + " type: " + rsmd.getColumnType(rsmdColumn));	// TODO Log this
					
					columns[i] = new Column(columnName, columnType);
				}
			} catch (SQLException e) {
				log.exceptionSevere(e);
			}
		}
		return columns;
	}
	
	/** 
	 * Returns the number of columns in this results.
	 * @return total number of columns
	 * @throws ClosedException if called on a closed resource
	 */
	public int getNumColumns() throws ClosedException {
		if (isClosed())
			throw new ClosedException();
		
		return getColumns().length;
	}
	
	/**
	 * Retrieves the next row of results.
	 * @return next row of results, or {@code null} if no more rows
	 * @throws ClosedException if called on a closed resource
	 */
	public RowEntry[] getNextRow() throws ClosedException {
		if (isClosed())
			throw new ClosedException();
			
		RowEntry[] row = null;
		try {
			if (rs.next()) {
				row = new RowEntry[getNumColumns()];
				
				for (int i = 0; i < row.length; i++) {
					row[i] = new RowEntry(getColumns()[i], setValue(rs, i));
				}
			}
		} catch (SQLException | MismatchedTypeException e) {
			log.exceptionSevere(e);
		}
		return row;
	}
	private Object setValue(ResultSet rs, int columnIndex) throws ClosedException {
		Object value = null;
		int rsIndex = columnIndex + 1;	// ResultSet index starts 1 ahead of column
		
		try {
			switch (getColumns()[columnIndex].getType()) {
			case BOOLEAN:
				value = rs.getBoolean(rsIndex);
				break;
			case SMALLINT:
				value = rs.getShort(rsIndex);
				break;
			case INTEGER:
				value = rs.getInt(rsIndex);
				break;
			case BIGINT:
				value = rs.getLong(rsIndex);
				break;
			case REAL:
				value = rs.getFloat(rsIndex);
				break;
			case DOUBLE:
				value = rs.getDouble(rsIndex);
				break;
			case CHAR:
				value = rs.getString(rsIndex).charAt(0);
				break;
			case VARCHAR:
				value = rs.getString(rsIndex);
				break;
			}
		} catch (SQLException e) {
			log.exceptionSevere(e);
		}
		return value;
	}
}
