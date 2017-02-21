package dev.kkorolyov.sqlob;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import dev.kkorolyov.sqlob.persistence.SessionTestOld;

@SuppressWarnings("javadoc")
@RunWith(Suite.class)

@SuiteClasses({ SessionTestOld.class})

public class AllTests {
	// Suite
}
