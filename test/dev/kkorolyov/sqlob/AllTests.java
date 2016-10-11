package dev.kkorolyov.sqlob;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import dev.kkorolyov.sqlob.connection.DatabaseConnectionTest;
import dev.kkorolyov.sqlob.connection.TableConnectionTest;
import dev.kkorolyov.sqlob.construct.ColumnTest;
import dev.kkorolyov.sqlob.construct.ResultsTest;
import dev.kkorolyov.sqlob.construct.EntryTest;
import dev.kkorolyov.sqlob.construct.SqlTypeTest;
import dev.kkorolyov.sqlob.statement.StatementBuilderTest;

@SuppressWarnings("javadoc")
@RunWith(Suite.class)

@SuiteClasses({ DatabaseConnectionTest.class,
								TableConnectionTest.class,
								ColumnTest.class, 
								ResultsTest.class,
								EntryTest.class,
								SqlTypeTest.class,
								StatementBuilderTest.class})

public class AllTests {
	// Suite
}
