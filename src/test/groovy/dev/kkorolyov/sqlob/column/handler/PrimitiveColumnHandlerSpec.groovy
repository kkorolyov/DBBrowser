package dev.kkorolyov.sqlob.column.handler

import dev.kkorolyov.simplefiles.Providers
import dev.kkorolyov.sqlob.type.SqlobType
import dev.kkorolyov.sqlob.type.factory.SqlobTypeFactory

import spock.lang.Specification

import static dev.kkorolyov.simplespecs.SpecUtilities.getField
import static dev.kkorolyov.simplespecs.SpecUtilities.setField

class PrimitiveColumnHandlerSpec extends Specification {
	static final Providers<SqlobType> ORIGINAL_SQLOB_TYPES = getField("SQLOB_TYPES", SqlobTypeFactory)

	SqlobType<?> sqlobType = Mock() {
		getTypes() >> [String]
	}

	PrimitiveColumnHandler handler = new PrimitiveColumnHandler()

	def cleanupSpec() {
		setField("SQLOB_TYPES", SqlobTypeFactory, ORIGINAL_SQLOB_TYPES)
	}

	def setup() {
		setField("SQLOB_TYPES", SqlobTypeFactory, Providers.fromInstances(SqlobType, [sqlobType] as Set))
	}

	def "accepts field with associated sqlob type"() {
		expect:
		handler.accepts(Stub.getDeclaredField("value"))
	}

	def "rejects field without associated sqlob type"() {
		expect:
		!handler.accepts(Stub.getDeclaredField("otherValue"))
	}

	class Stub {
		String value
		Integer otherValue
	}
}
