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

	@Test
	public void testHashCode() {
		String testName = "HashTest";
		
		for (SqlType testType : SqlType.values()) {
			Column column1 = new Column(testName, testType), column2 = new Column(testName, testType);
			assertEquals(column1.hashCode(), column2.hashCode());
		}
	}
	@Test
	public void testEquals() {
		String testName = "EqualsTest", testNameLower = testName.toLowerCase(), testNameUpper = testName.toUpperCase(), testName2 = "EqualsTest2";
		
		for (SqlType testType : SqlType.values()) {
			Column column1 = new Column(testName, testType), column2 = new Column(testName, testType), column3 = new Column(testName2, testType);
			Column columnLower = new Column(testNameLower, testType), columnUpper = new Column(testNameUpper, testType);
			assertEquals(column1, column2);
			assertEquals(column1.hashCode(), column2.hashCode());	// Test equals-hashcode contract 
			assertEquals(column1, columnLower);
			assertEquals(column1.hashCode(), columnLower.hashCode());
			assertEquals(column1, columnUpper);
			assertEquals(column1.hashCode(), columnUpper.hashCode());
			
			assertNotEquals(column1, column3);
			assertNotEquals(column2, column3);
		}
	}
}
