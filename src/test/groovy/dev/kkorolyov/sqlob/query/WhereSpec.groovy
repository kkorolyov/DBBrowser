package dev.kkorolyov.sqlob.query

import dev.kkorolyov.sqlob.util.Where

import spock.lang.Shared
import spock.lang.Specification

class WhereSpec extends Specification {
	@Shared String attribute = "FakeAttr"
	@Shared String operator = "FakeOp"
	@Shared def values = ["String", 54, 424.44, new Object()]

	Where condition

	Where generateCondition(int num) {
		return new Where("$attribute $num", "$operator $num", "$values $num")
	}

	def "no-arg condition is empty"() {
		when:
		condition = new Where()

		then:
		condition.toString().length() == 0
		condition.values().size() == 0
	}

	def "values added to values list"() {
		when:
		condition = new Where(attribute, operator, value)

		then:
		condition.values().size() == 1
		condition.values().contains(value)

		where:
		value << values
	}
	def "null value not added to values list"() {
		when:
		condition = new Where(attribute, operator, null)

		then:
		condition.values().size() == 0
	}

	def "toString()s to 'attribute operator ?' format"() {
		when:
		condition = new Where(attribute, operator, value)

		then:
		condition.toString() == "$attribute $operator ?"

		where:
		value << values
	}
	def "null value toString()s to NULL instead of ?"() {
		when:
		condition = new Where(attribute, operator, null)

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
		Where inner = generateCondition(1)

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
		Where chained = generateCondition(1)

		when:
		condition = new Where()
		condition.and(chained)

		then:
		condition.toString().contains(chained.toString())
		!condition.toString().contains('AND')

		when:
		condition = new Where()
		condition.or(chained)

		then:
		condition.toString().contains(chained.toString())
		!condition.toString().contains('OR')
	}
}
