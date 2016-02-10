package dev.kkorolyov.ezdb.construct;

import static org.junit.Assert.*;

import org.junit.Test;

import dev.kkorolyov.ezdb.construct.SqlType;

@SuppressWarnings("javadoc")
public class SqlTypeTest {

	@Test
	public void testGetTypeName() {
		String[][] testNames = {{"BOOLEAN",						SqlType.BOOLEAN.getTypeName()},
														{"SMALLINT",					SqlType.SMALLINT.getTypeName()},
														{"INTEGER", 					SqlType.INTEGER.getTypeName()},
														{"BIGINT", 						SqlType.BIGINT.getTypeName()},
														{"REAL", 							SqlType.REAL.getTypeName()},
														{"DOUBLE PRECISION",	SqlType.DOUBLE.getTypeName()},
														{"CHAR", 							SqlType.CHAR.getTypeName()},
														{"VARCHAR",						SqlType.VARCHAR.getTypeName()}};
		
		testProperties(testNames);
	}

	@Test
	public void testGetTypeCode() {
		Integer[][] testTypes = {	{java.sql.Types.BOOLEAN, 	SqlType.BOOLEAN.getTypeCode()},
															{java.sql.Types.SMALLINT, SqlType.SMALLINT.getTypeCode()},
															{java.sql.Types.INTEGER, 	SqlType.INTEGER.getTypeCode()},
															{java.sql.Types.BIGINT, 	SqlType.BIGINT.getTypeCode()},
															{java.sql.Types.REAL, 		SqlType.REAL.getTypeCode()},
															{java.sql.Types.DOUBLE, 	SqlType.DOUBLE.getTypeCode()},
															{java.sql.Types.CHAR, 		SqlType.CHAR.getTypeCode()},
															{java.sql.Types.VARCHAR, 	SqlType.VARCHAR.getTypeCode()}};

		testProperties(testTypes);
	}

	@Test
	public void testGetTypeClass() {
		Class<?>[][] testClasses = {{Boolean.class, 	SqlType.BOOLEAN.getTypeClass()},
																{Short.class, 		SqlType.SMALLINT.getTypeClass()},
																{Integer.class,		SqlType.INTEGER.getTypeClass()},
																{Long.class, 			SqlType.BIGINT.getTypeClass()},
																{Float.class, 		SqlType.REAL.getTypeClass()},
																{Double.class, 		SqlType.DOUBLE.getTypeClass()},
																{Character.class, SqlType.CHAR.getTypeClass()},
																{String.class, 		SqlType.VARCHAR.getTypeClass()}};

		testProperties(testClasses);
	}

	private static void testProperties(Object[][] expectedActualPairs) {
		if (expectedActualPairs.length < SqlType.values().length)
			fail("Tests are not written for " + (SqlType.values().length - expectedActualPairs.length) + " types");
		
		for (Object[] testPair : expectedActualPairs)
			assertEquals(testPair[0], testPair[1]);
	}
}
