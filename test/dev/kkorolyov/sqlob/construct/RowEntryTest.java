package dev.kkorolyov.sqlob.construct;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import dev.kkorolyov.sqlob.construct.Column;
import dev.kkorolyov.sqlob.construct.Entry;
import dev.kkorolyov.sqlob.construct.SqlType;

@SuppressWarnings("javadoc")
public class RowEntryTest {
	private static final Map<SqlType, Object> matchedTypes = new HashMap<>();
	
	static {	// Load matching types
		matchedTypes.put(SqlType.BOOLEAN, false);
		
		matchedTypes.put(SqlType.SMALLINT, (short) 0);
		matchedTypes.put(SqlType.INTEGER, 0);
		matchedTypes.put(SqlType.BIGINT, (long) 0);
		matchedTypes.put(SqlType.REAL, (float) 0.0);
		matchedTypes.put(SqlType.DOUBLE, 0.0);
		
		matchedTypes.put(SqlType.CHAR, 'A');
		matchedTypes.put(SqlType.VARCHAR, "String");
		
		assert (matchedTypes.size() == SqlType.values().length);
	}
	
	@Test
	public void testConstructorMatchedType() throws MismatchedTypeException {
		for (SqlType type : SqlType.values())
			new Entry(new Column(type.getTypeName(), type), matchedTypes.get(type));
	}
	@Test
	public void testConstructorMismatchedType() {
		SqlType[] testTypes = SqlType.values();
		
		for (int i = 0; i < testTypes.length; i++) {
			SqlType testType = testTypes[i];
			Object testValue = i < testTypes.length / 2 ? matchedTypes.get(testTypes[i + 1]) : matchedTypes.get(testTypes[i - 1]);	// Avoid out of bounds exceptions
			
			try {
				new Entry(new Column(testType.getTypeName(), testType), testValue);
			} catch (MismatchedTypeException e) {
				continue;
			}
			fail("Did not throw a " + MismatchedTypeException.class.getSimpleName());
		}
	}

	@Test
	public void testGetColumn() throws MismatchedTypeException {
		for (SqlType type : SqlType.values()) {
			Column expectedColumn = new Column(type.getTypeName(), type);
			Column actualColumn = new Entry(expectedColumn, matchedTypes.get(type)).getColumn();
			
			assertEquals(expectedColumn, actualColumn);
		}
	}

	@Test
	public void testGetValue() throws MismatchedTypeException {
		for (SqlType type : SqlType.values()) {
			Object expectedValue = matchedTypes.get(type);
			Object actualValue = new Entry(new Column(type.getTypeName(), type), expectedValue).getValue();
			
			assertEquals(expectedValue, actualValue);
		}
	}

	@Test
	public void testHashCode() throws MismatchedTypeException {
		Column testColumn = new Column("TestColumn", SqlType.BOOLEAN);
		Boolean testValue = false;
		
		Entry entry1 = new Entry(testColumn, testValue), entry2 = new Entry(testColumn, testValue);
		assertEquals(entry1, entry2);
	}
	@Test
	public void testEquals() throws MismatchedTypeException {
		Column testColumn = new Column("TestColumn", SqlType.BOOLEAN);
		Boolean testValue = false, testValue2 = true;
		
		Entry entry1 = new Entry(testColumn, testValue), entry2 = new Entry(testColumn, testValue), entry3 = new Entry(testColumn, testValue2);
		assertEquals(entry1, entry2);
		assertEquals(entry1.hashCode(), entry2.hashCode());
		
		assertNotEquals(entry1, entry3);
		assertNotEquals(entry1, entry3);
	}
}
