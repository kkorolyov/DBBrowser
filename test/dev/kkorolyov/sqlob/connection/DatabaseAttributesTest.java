package dev.kkorolyov.sqlob.connection;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import dev.kkorolyov.sqlob.construct.SqlobType;

@SuppressWarnings("javadoc")
public class DatabaseAttributesTest {
	private static final String[] validFiles = new String[]{"sqlobfiles/postgresql.sqlob",
																													"sqlobfiles/sqlite.sqlob"};

	@Test
	public void testValidSqlobfile() throws ClassNotFoundException {
		for (String filename : validFiles) {
			DatabaseAttributes attr = new DatabaseAttributes(new File(filename));
			
			for (SqlobType supportedType : attr.getTypes()) {
				assertEquals(supportedType, attr.getTypes().get(supportedType.getTypeClass()));
				assertEquals(supportedType, attr.getTypes().get(supportedType.getTypeCode()));
			}
		}
	}
}
