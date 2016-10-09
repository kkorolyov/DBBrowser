package dev.kkorolyov.sqlob.connection;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class DatabaseAttributesTest {
	private static final String[] validFiles = new String[]{"sqlobfiles/postgresql.sqlob",
																													"sqlobfiles/sqlite.sqlob"};

	@Test
	public void testValidSqlobfile() throws ClassNotFoundException {
		for (String filename : validFiles) {
			DatabaseAttributes attr = DatabaseAttributes.get(filename);
			
			System.out.println(filename);
			System.out.println(attr.getDriverName());
			System.out.println(attr.getURL("h", "d"));
		}
	}
}
