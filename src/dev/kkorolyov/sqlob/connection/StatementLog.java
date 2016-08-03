package dev.kkorolyov.sqlob.connection;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import dev.kkorolyov.sqlob.construct.statement.StatementCommand;
import dev.kkorolyov.sqlob.construct.statement.UpdateStatement;

/**
 * Logs executed {@code StatementCommands}.
 * @see StatementCommand
 */
public class StatementLog implements Iterable<StatementCommand> {
	private List<StatementCommand> statements = new LinkedList<>();

	/**
	 * Returns the statement at the specified index.
	 * @param index index of statement to return
	 * @return statement at {@code index}
	 * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
	 */
	public StatementCommand get(int index) {
		return statements.get(index);
	}
	/**
	 * @return first statement in this log
	 * @throws IndexOutOfBoundsException if this log is empty
	 */
	public StatementCommand getFirst() {
		return get(0);
	}
	/**
	 * @return last statement in this log
	 * @throws IndexOutOfBoundsException if this log is empty
	 */
	public StatementCommand getLast() {
		return get(size() - 1);
	}
	
	/**
	 * Adds a statement to the end of this log.
	 * @param statement statement to add
	 */
	public void add(StatementCommand statement) {
		statements.add(statement);
	}
	/**
	 * Removes the first occurrence of a statement from this log.
	 * @param statement statement to remove
	 */
	public void remove(StatementCommand statement) {
		statements.remove(statement);
	}
	
	/**
	 * Clears all statements from this log.
	 */
	public void clear() {
		statements.clear();
	}
	
	/**
	 * Reverts a statement if it is revertible.
	 * @param statement statement to revert
	 * @param remove if {@code true}, will remove the first occurrence of {@code statement} from this log after reverting
	 */
	public void revert(UpdateStatement statement, boolean remove) {
		UpdateStatement inverse = statement.getInverseStatement();
		
		if (inverse != null) {
			inverse.execute();	// Statement will not be logged
			
			if (remove)
				remove(statement);
		}
	}
	
	/** @return {@code true} if this log contains no statements */
	public boolean isEmpty() {
		return statements.isEmpty();
	}
	
	/** @return number of statements in this log */
	public int size() {
		return statements.size();
	}
	
	@Override
	public Iterator<StatementCommand> iterator() {
		return statements.iterator();
	}
}
