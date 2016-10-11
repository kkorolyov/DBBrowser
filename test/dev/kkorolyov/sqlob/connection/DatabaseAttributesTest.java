package dev.kkorolyov.sqlob.connection;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import dev.kkorolyov.sqlob.construct.SqlobType;

@RunWith(Parameterized.class)
@SuppressWarnings("javadoc")
public class DatabaseAttributesTest {
	@Parameters
	public static Object[] data() {
		return new Object[]{"sqlobfiles/postgresql.sqlob",
												"sqlobfiles/sqlite.sqlob"};
	}
	private final File file;

	public DatabaseAttributesTest(String input) {
		file = new File(input);
	}
	
	@Test
	public void testValidSqlobfile() {
		DatabaseAttributes attr = new DatabaseAttributes(file);
		
		for (SqlobType supportedType : attr.getTypes()) {
			assertEquals(supportedType, attr.getTypes().get(supportedType.getTypeClass()));
			//assertEquals(supportedType, attr.getTypes().get(supportedType.getTypeCode()));	// TODO Code->Class may be 1->many
		}
	}
}
