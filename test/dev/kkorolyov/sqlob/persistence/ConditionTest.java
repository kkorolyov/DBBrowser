package dev.kkorolyov.sqlob.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;

class ConditionTest {
	private static final String DEFAULT_ATTRIBUTE = "Something";
	private static final String DEFAULT_OPERATOR = "=";
	private static final Object DEFAULT_VALUE = "val";

	private static final String VALUE_PLACEHOLDER = "?";

	private static final Iterable<String> attributes = Arrays.asList("pizza", "pie", "pasta", "no");
	private static final Iterable<String> operators = Arrays.asList("=", "!=", ">", ">=", "<", "<=", "IS", "NOT", "NOPE");
	private static final Iterable<Object> values = Arrays.asList("val", 0, 555325235235L, .14, UUID.randomUUID(), new Condition());

	private Condition condition;

	@Test
	void noArgConstructedIsEmpty() {
		condition = new Condition();

		assertEquals(0, condition.toString().length());
		assertFalse(condition.values().iterator().hasNext());
	}

	@Test
	void nullValueNotAddedToValues() {
		condition = new Condition("something", "is", null);

		assertFalse(condition.values().iterator().hasNext());
	}

	@TestFactory
	Stream<DynamicTest> toStringContainsAttribute() {
		return DynamicTest.stream(attributes.iterator(),
															attribute -> "Attribute: " + attribute,
															attribute -> assertTrue(new Condition(attribute, DEFAULT_OPERATOR, DEFAULT_VALUE)
																													.toString().contains(attribute)));
	}
	@TestFactory
	Stream<DynamicTest> toStringContainsOperator() {
		return DynamicTest.stream(operators.iterator(),
															operator -> "Operator: " + operator,
															operator -> assertTrue(new Condition(DEFAULT_ATTRIBUTE, operator, DEFAULT_VALUE)
																												 .toString().contains(operator)));
	}
	@TestFactory
	Stream<DynamicTest> toStringContainsValuePlaceholder() {
		return DynamicTest.stream(values.iterator(),
															value -> "Value: " + value,
															value -> assertTrue(new Condition(DEFAULT_ATTRIBUTE, DEFAULT_OPERATOR, value)
																											.toString().contains(VALUE_PLACEHOLDER)));
	}

	@Test
	void emptyAndsWithoutAnd() {
		condition = new Condition();
		Condition appended = generateDefaultCondition();

		condition.and(appended);

		assertFalse(condition.toString().contains("AND"));
	}
	@Test
	void emptyOrsWithoutOr() {
		condition = new Condition();
		Condition appended = generateDefaultCondition();

		condition.or(appended);

		assertFalse(condition.toString().contains("OR"));
	}

	@Test
	void andAppendsWithGrouping() {
		condition = generateDefaultCondition();
		Condition inner = generateDefaultCondition();

		condition.and(inner);

		assertTrue(condition.toString().contains(group(inner)));
	}
	@Test
	void orAppendsWithGrouping() {
		condition = generateDefaultCondition();
		Condition inner = generateDefaultCondition();

		condition.or(inner);

		assertTrue(condition.toString().contains(group(inner)));
	}

	@Test
	void andAppendsCompoundConditionsWithGrouping() {
		condition = generateDefaultCondition();
		Condition inner1 = generateDefaultCondition(), inner2 = generateDefaultCondition();

		condition.and(inner1.or(inner2));

		assertTrue(condition.toString().contains(group(inner1)));
	}
	@Test
	void orAppendsCompoundConditionsWithGrouping() {
		condition = generateDefaultCondition();
		Condition inner1 = generateDefaultCondition(), inner2 = generateDefaultCondition();

		condition.or(inner1.and(inner2));

		assertTrue(condition.toString().contains(group(inner1)));
	}

	private static final String group(Condition condition) {
		return "(" + condition.toString() + ")";
	}

	private static Condition generateDefaultCondition() {
		return new Condition(DEFAULT_ATTRIBUTE, DEFAULT_OPERATOR, DEFAULT_VALUE);
	}
}
