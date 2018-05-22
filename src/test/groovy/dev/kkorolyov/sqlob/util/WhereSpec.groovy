package dev.kkorolyov.sqlob.util

import spock.lang.Shared
import spock.lang.Specification

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class WhereSpec extends Specification {
	@Shared String attribute = randString()
	@Shared String operator = randString()
	@Shared Object value = Mock()

	Where where = new Where(attribute, operator, value)

	def "appends other where with delimiter"() {
		Where original = new Where(attribute, operator, value)
		Where other = new Where(randString(), randString(), Mock(Object))

		when:
		where."$method"(other)

		then:
		where.getSql() == "${original.getSql()} $delimiter (${other.getSql()})"

		where:
		method << ["and", "or"]
		delimiter << ["AND", "OR"]
	}

	def "consumes attribute values in order"() {
		List<String> values = (0..4).collect { randString() }
		Where where = values.inject(new Where(randString(), randString(), randString())) { where, value ->
			where.and(attribute, operator, value)
			where.and(randString(), operator, randString())
		}
		List<String> results = []

		when:
		where.consumeValues(attribute) { index, value -> results.add(value) }

		then:
		values == results
	}

	def "formats SQL string with wildcard"() {
		expect:
		where.getSql() == "$attribute $operator ?"
	}
	def "formats toString() with value"() {
		expect:
		where.toString() == "$attribute $operator $value"
	}
}
