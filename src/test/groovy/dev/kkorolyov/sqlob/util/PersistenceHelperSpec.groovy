package dev.kkorolyov.sqlob.util

import dev.kkorolyov.sqlob.annotation.Column
import dev.kkorolyov.sqlob.annotation.Table
import dev.kkorolyov.sqlob.annotation.Transient

import spock.lang.Specification

import java.lang.reflect.Field
import java.util.stream.Collectors

class PersistenceHelperSpec extends Specification {
	def "gets simple name of non-Table-annotated class"() {
		expect:
		PersistenceHelper.getName(Simple) == Simple.getSimpleName()
	}
	def "gets provided name of Table-annotated class"() {
		expect:
		PersistenceHelper.getName(Annotated) == "TABLE"
	}

	def "gets simple name of type of non-Column-annotated field"() {
		expect:
		PersistenceHelper.getName(Simple.getField()) == Simple.getField().getName()
	}
	def "gets provided name of Column-annotated field"() {
		expect:
		PersistenceHelper.getName(Annotated.getField()) == "COLUMN"
	}

	def "static field not persistable"() {
		expect:
		!PersistenceHelper.isPersistable(Simple.getStaticField())
	}
	def "transient field not persistable"() {
		expect:
		!PersistenceHelper.isPersistable(Simple.getTransientField())
	}
	def "Transient-annotated field not persistable"() {
		expect:
		!PersistenceHelper.isPersistable(Annotated.getTransientField())
	}
	def "non-static non-transient non-Transient non-synthetic field persistable"() {
		expect:
		PersistenceHelper.isPersistable(Simple.getField())
		PersistenceHelper.isPersistable(Annotated.getField())
	}

	def "filters persistable fields"() {
		expect:
		PersistenceHelper.getPersistableFields(Simple).collect(Collectors.toSet()) == [Simple.getField()] as Set
		PersistenceHelper.getPersistableFields(Annotated).collect(Collectors.toSet()) == [Annotated.getField()] as Set
	}

	private static class Simple {
		private String value
		private static String staticValue
		private transient String transientValue

		static Field getField() {
			Simple.getDeclaredField("value")
		}
		static Field getStaticField() {
			Simple.getDeclaredField("staticValue")
		}
		static Field getTransientField() {
			Simple.getDeclaredField("transientValue")
		}
	}
	@Table("TABLE")
	private static class Annotated {
		@Column("COLUMN")
		private String value
		@Transient
		private String transientValue

		static Field getField() {
			Annotated.getDeclaredField("value")
		}
		static Field getTransientField() {
			Annotated.getDeclaredField("transientValue")
		}
	}
}
