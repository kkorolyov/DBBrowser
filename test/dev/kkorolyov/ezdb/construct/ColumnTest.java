package dev.kkorolyov.ezdb.construct;

import static org.junit.Assert.*;

import org.junit.Test;

@SuppressWarnings("javadoc")
public class ColumnTest {

	@Test
	public void testColumn() {	// Sanity test for each possible type
		for (SqlType type : SqlType.values())
			new Column(type.getTypeName(), type);
	}

	@Test
	public void testGetName() {
		String[] testNames = {"test", "TEST", "Test"};
		
		for (String testName : testNames) {
			for (SqlType type : SqlType.values())
				assertEquals(testName, new Column(testName, type).getName());
		}
	}

	@Test
	public void testGetType() {
		String testName = "Test";
		
		for (SqlType type : SqlType.values())
			assertEquals(type, new Column(testName, type).getType());
	}

}
