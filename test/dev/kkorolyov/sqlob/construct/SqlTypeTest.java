package dev.kkorolyov.sqlob.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import dev.kkorolyov.sqlob.construct.SqlType;

@SuppressWarnings("javadoc")
public class SqlTypeTest {
	private static final SqlType[] sqlTypes = {	SqlType.BOOLEAN,
																							SqlType.SMALLINT,
																							SqlType.INTEGER,
																							SqlType.BIGINT,
																							SqlType.REAL,
																							SqlType.DOUBLE,
																							SqlType.CHAR,
																							SqlType.VARCHAR};
	
	private static final String[] names = {	"BOOLEAN",
																					"SMALLINT",
																					"INTEGER",
																					"BIGINT",
																					"REAL",
																					"DOUBLE PRECISION",
																					"CHAR",
																					"VARCHAR"};
	
	private static final int[] typeCodes = {java.sql.Types.BIT,
																			java.sql.Types.SMALLINT,
																			java.sql.Types.INTEGER,
																			java.sql.Types.BIGINT,
																			java.sql.Types.REAL,
																			java.sql.Types.DOUBLE,
																			java.sql.Types.CHAR,
																			java.sql.Types.VARCHAR};
	
	private static final Class<?>[] classes = {	Boolean.class,
																							Short.class,
																							Integer.class,
																							Long.class,
																							Float.class,
																							Double.class,
																							Character.class,
																							String.class};

	@Test
	public void testGetTypeName() {		
		isTestingAll();
		
		for (int i = 0; i < sqlTypes.length; i++)
			assertEquals(names[i], sqlTypes[i].getTypeName());
	}

	@Test
	public void testGetTypeCode() {
		isTestingAll();
		
		for (int i = 0; i < sqlTypes.length; i++)
			assertEquals(typeCodes[i], sqlTypes[i].getTypeCode());
	}

	@Test
	public void testGetTypeClass() {
		isTestingAll();
		
		for (int i = 0; i < sqlTypes.length; i++)
			assertEquals(classes[i], sqlTypes[i].getTypeClass());
	}
	
	@Test
	public void testGet() {
		isTestingAll();
		
		for (int i = 0; i < sqlTypes.length; i++) {
			int testTypeCode = typeCodes[i];
			SqlType testType = SqlType.get(testTypeCode);
			
			assertEquals(sqlTypes[i], testType);
		}
	}

	private static void isTestingAll() {
		if (sqlTypes.length < SqlType.values().length)
			fail("Tests are not written for " + (SqlType.values().length - sqlTypes.length) + " types");
	}
}
