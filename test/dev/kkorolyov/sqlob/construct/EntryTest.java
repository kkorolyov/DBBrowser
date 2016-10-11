package dev.kkorolyov.sqlob.construct;

import static org.junit.Assert.*;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class EntryTest {
	@Parameters
	public static Object[] data() {
		return new Object[]{new SqlobType(Boolean.class, "BOOLEAN", Types.BOOLEAN),
												new SqlobType(Short.class, "SMALLINT", Types.SMALLINT),
												new SqlobType(Integer.class, "INTEGER", Types.INTEGER),
												new SqlobType(Long.class, "BIGINT", Types.BIGINT),
												new SqlobType(Float.class, "REAL", Types.REAL),
												new SqlobType(Double.class, "DOUBLE PRECISION", Types.DOUBLE),
												new SqlobType(Character.class, "CHAR", Types.CHAR),
												new SqlobType(String.class, "VARCHAR", Types.VARCHAR)};
	}
	private final SqlobType type;
	
	public EntryTest(SqlobType input) {
		type = input;
	}
	
	@Test
	public void testHasParameter() {
		List<Entry> haveParameters = new ArrayList<>(),
								notHaveParameters = new ArrayList<>();
		
		haveParameters.add(new Entry(getStubColumn(), getStubValue()));
		notHaveParameters.add(new Entry(getStubColumn(), null));

		for (Entry.Operator operator : Entry.Operator.values()) {
			haveParameters.add(new Entry(getStubColumn(), operator, getStubValue()));
			
			notHaveParameters.add(new Entry(getStubColumn(), operator, null));
			notHaveParameters.add(new Entry(getStubColumn(), operator, getStubColumn()));
		}
		for (Entry entry : haveParameters)
			assertTrue(entry.hasParameter());
		for (Entry entry : notHaveParameters)
			assertFalse(entry.hasParameter());
	}

	private Column getStubColumn() {
		return new Column("TABLE", "COLUMN", type);
	}
	
	private Object getStubValue() {
		if (type.getTypeClass() == Boolean.class)
			return false;
		else if (type.getTypeClass() == Short.class)
			return (short) 0;
		else if (type.getTypeClass() == Integer.class)
			return (int) 0;
		else if (type.getTypeClass() == Long.class)
			return (long) 0;
		else if (type.getTypeClass() == Float.class)
			return (float) 0;
		else if (type.getTypeClass() == Double.class)
			return (double) 0;
		else if (type.getTypeClass() == Character.class)
			return 'a';
		else if (type.getTypeClass() == String.class)
			return "A";
		else
			return null;	// Should not happen
	}
}
