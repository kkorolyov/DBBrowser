package dev.kkorolyov.sqlob.utility

import spock.lang.Shared
import spock.lang.Specification

class ConditionSpec extends Specification {
	@Shared String attribute = "FakeAttr"
	@Shared String operator = "FakeOp"
	@Shared def values = ["String", 54, 424.44, new Object()]

	Condition condition

	Condition generateCondition(int num) {
		return new Condition("$attribute $num", "$operator $num", "$values $num")
	}

	def "no-arg condition is empty"() {
		when:
		condition = new Condition()

		then:
		condition.toString().length() == 0
		condition.values().size() == 0
	}

	def "values added to values list"() {
		when:
		condition = new Condition(attribute, operator, value)

		then:
		condition.values().size() == 1
		condition.values().contains(value)

		where:
		value << values
	}
	def "null value not added to values list"() {
		when:
		condition = new Condition(attribute, operator, null)

		then:
		condition.values().size() == 0
	}

	def "toString()s to 'attribute operator ?' format"() {
		when:
		condition = new Condition(attribute, operator, value)

		then:
		condition.toString() == "$attribute $operator ?"

		where:
		value << values
	}
	def "null value toString()s to NULL instead of ?"() {
		when:
		condition = new Condition(attribute, operator, null)

		then:
		condition.toString() == "$attribute $operator NULL"
	}

	def "and() chains using AND"() {
		condition = generateCondition(0)

		when:
		condition.and(generateCondition(1))

		then:
		condition.toString().contains('AND')
	}
	def "or() chains using OR"() {
		condition = generateCondition(0)

		when:
		condition.or(generateCondition(1))

		then:
		condition.toString().contains('OR')
	}

	def "chains using () grouping"() {
		Condition inner = generateCondition(1)

		when:
		condition = generateCondition(0)
		condition.and(inner)

		then:
		condition.toString().contains("AND ($inner)")

		when:
		condition = generateCondition(0)
		condition.or(inner)

		then:
		condition.toString().contains("OR ($inner)")
	}

	def "chaining to empty condition results in chained condition without AND/OR"() {
		Condition chained = generateCondition(1)

		when:
		condition = new Condition()
		condition.and(chained)

		then:
		condition.toString().contains(chained.toString())
		!condition.toString().contains('AND')

		when:
		condition = new Condition()
		condition.or(chained)

		then:
		condition.toString().contains(chained.toString())
		!condition.toString().contains('OR')
	}
}
