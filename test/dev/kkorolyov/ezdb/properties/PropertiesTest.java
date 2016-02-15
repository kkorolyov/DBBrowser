package dev.kkorolyov.ezdb.properties;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class PropertiesTest {

	@AfterClass
	public static void tearDownAfterClass() {
		Properties.loadDefaults();
		Properties.saveToFile();
	}
	
	@Test
	public void testLoadDefaults() {
		Properties.loadDefaults();
		assertEquals(Properties.DEFAULT_HOST, Properties.getValue(Properties.HOST));
		assertEquals(Properties.DEFAULT_USER, Properties.getValue(Properties.USER));
		assertEquals(Properties.DEFAULT_PASSWORD, Properties.getValue(Properties.PASSWORD));
	}

	@Test
	public void testLoadFile() {
		Properties.loadFile();
		for (String key : Properties.getAllKeys())
			System.out.println(key + "=" + Properties.getValue(key));
	}

	@Test
	public void testSaveToFile() {
		String testKey = Properties.HOST, testValue = "TEST_HOST";
		
		Properties.loadDefaults();
		Properties.addProperty(testKey, testValue);
		Properties.saveToFile();
		
		Properties.loadDefaults();
		assertTrue(!Properties.getValue(testKey).equals(testValue));
		Properties.loadFile();
		assertEquals(testValue, Properties.getValue(testKey));
	}

	@Test
	public void testGetValue() {
		String testKey = "TEST_KEY", testValue = "TEST_VALUE";
		Properties.addProperty(testKey, testValue);
		assertEquals(testValue, Properties.getValue(testKey));
	}

}
