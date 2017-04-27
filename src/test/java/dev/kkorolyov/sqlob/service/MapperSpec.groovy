package dev.kkorolyov.sqlob.service

import dev.kkorolyov.sqlob.NonPersistableException
import dev.kkorolyov.sqlob.annotation.Column
import dev.kkorolyov.sqlob.annotation.Table
import dev.kkorolyov.sqlob.annotation.Transient
import dev.kkorolyov.sqlob.utility.Converter
import dev.kkorolyov.sqlob.utility.Extractor
import groovy.transform.PackageScope
import spock.lang.Shared
import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.ResultSet

class MapperSpec extends Specification {
  @Shared String stubSqlType = Constants.sanitize("SomeSQL")
	@Shared Converter stubConverter = { o -> o }
	@Shared Extractor stubExtractor = { rs, column -> rs.getString(column) }

  Mapper mapper = new Mapper()

	class Empty {}
	class Multi {
		Empty e1
		@PackageScope Empty e2
		private Empty e3
	}

	def "returns typemapped sqlType"() {
		Class c = Empty
		String sqlType = stubSqlType

		when:
		mapper.put(c, sqlType, stubExtractor)

		then:
		mapper.sql(c) == sqlType
		mapper.sql(f) == sqlType

		where:
		f << [Multi.getDeclaredField("e1"),
					Multi.getDeclaredField("e2"),
					Multi.getDeclaredField("e3")]
	}

	def "sqlType is sanitized"() {
		Class<?> c = Empty
		String sqlType = "Bad SQ;L"

		when:
		mapper.put(c, sqlType, stubConverter, stubExtractor)

		then:
		mapper.sql(c) != sqlType
		mapper.sql(c) == Constants.sanitize(sqlType)
	}

	def "converts using typemapped converter"() {
		Class c = Empty
		Converter converter = { o -> "HI" }
		Object o = c.newInstance()

		when:
		mapper.put(c, stubSqlType, converter, stubExtractor)

		then:
		mapper.convert(o) != o
		mapper.convert(o) == converter.execute(o)
	}
	def "no conversion if no typemapped converter"() {
		Class c = Empty
		Object o = c.newInstance()

		expect:
		mapper.convert(o) == o
	}

	def "extracts using typemapped extractor"() {
		Class c = Empty
		Extractor extractor = { rs, column -> rs.getObject(column) }
		String column = "TestCol"

		when:
		Empty expected = new Empty()
		ResultSet rs = Mock()
		1 * rs.getObject(column) >> expected
		1 * rs.getObject(mapper.getName(f)) >> expected

		mapper.put(c, stubSqlType, extractor)

		then:
		mapper.extract(c, rs, column) == expected
		mapper.extract(f, rs) == expected

		where:
		f << [Multi.getDeclaredField("e1"),
					Multi.getDeclaredField("e2"),
					Multi.getDeclaredField("e3")]
	}

	def "persistable fields are one of each basic field"() {
    expect:
    Iterable<Field> results = mapper.getPersistableFields(Multi)

    results.containsAll([Multi.getDeclaredField("e1"),
												 Multi.getDeclaredField("e2"),
												 Multi.getDeclaredField("e3")])
    results.size() == 3
  }

	static class TransientTag {
		@Transient
		private Empty e
	}
	static class TransientTagPlusOne {
		@Transient
		private Empty e1
		private Empty e2
	}
  def "Transient-tagged fields are not persistable"() {
    expect:
		Iterable<Field> fields = mapper.getPersistableFields(c)

		fields.size() == persistable.size()
		for (Field f : persistable) fields.contains(f)

    where:
    c << [TransientTag, TransientTagPlusOne]
		persistable << [[],
										[TransientTagPlusOne.getDeclaredField("e2")]]
  }

	static class TransientModifier {
		private transient Empty e
	}
	static class TransientModifierPlusOne {
		private transient Empty e1
		private Empty e2
	}
  def "transient fields are not persistable"() {
    expect:
    Iterable<Field> fields = mapper.getPersistableFields(c)

		fields.size() == persistable.size()
		for (Field f : persistable) fields.contains(f)

    where:
    c << [TransientModifier, TransientModifierPlusOne]
    persistable << [[],
										[TransientModifierPlusOne.getDeclaredField("e2")]]
  }

	static class StaticModifier {
		private static Empty e
	}
	static class StaticModifierPlusOne {
		private static Empty e1
		private Empty e2
	}
  def  "static fields are not persistable"() {
    expect:
    Iterable<Field> fields = mapper.getPersistableFields(c)

		fields.size() == persistable.size()
		for (Field f : persistable) fields.contains(f)

    where:
    c << [StaticModifier, StaticModifierPlusOne]
		persistable << [[],
										[StaticModifierPlusOne.getDeclaredField("e2")]]
  }

	class SelfRef {
		SelfRef selfRef1;
		@PackageScope SelfRef selfRef2
		private SelfRef selfRef2
	}
	class RefLoop1 {
		RefLoop2 ref
	}
	class RefLoop2 {
		RefLoop1 ref
	}
  def "associated classes composed of the given class and its distinct field types"() {
    expect:
    Iterable<Class<?>> results = mapper.getAssociatedClasses(c)

    results.containsAll(classes)
    results.size() == classes.size()

    where:
    c << [Multi, SelfRef, RefLoop1, RefLoop2]
    classes << [[Multi, Empty],
                [SelfRef],
                [RefLoop1, RefLoop2],
                [RefLoop1, RefLoop2]]
  }
	def "given class is last associated class"() {
		expect:
		mapper.getAssociatedClasses(c).last() == c

		where:
		c << [Multi, SelfRef, RefLoop1, RefLoop2]
	}

	def "typemapped classes are primitive"() {
		Class<?> c = Empty

		when:
		mapper.put(c, stubSqlType, stubExtractor)

		then:
		mapper.isPrimitive(c)
		!mapper.isComplex(c)

		mapper.isPrimitive(f)
		!mapper.isComplex(f)

		where:
		f << [Multi.getDeclaredField("e1"),
					Multi.getDeclaredField("e2"),
					Multi.getDeclaredField("e3")]
	}
	def "non-typemapped classes are complex"() {
		Class<?> c = Empty

		expect:
		mapper.isComplex(c)
		!mapper.isPrimitive(c)

		mapper.isComplex(f)
		!mapper.isPrimitive(f)

		where:
		f << [Multi.getDeclaredField("e1"),
					Multi.getDeclaredField("e2"),
					Multi.getDeclaredField("e3")]
	}

	static class NonTagged {
		String s
	}
	@Table("CustomTable") static class Tagged {
		@Column("CustomColumn") String s
	}
	@Table("") static class EmptyTagged {
		@Column("") String s
	}

	def "getName(Class) returns simple name of non-Table-tagged class"() {
		expect:
		mapper.getName(c) == c.getSimpleName()

		where:
		c << [NonTagged]
	}
	def "getName(Class) returns custom name of Table-tagged class"() {
		expect:
		mapper.getName(c) == name

		where:
		c << [Tagged]
		name << ["CustomTable"]
	}
	def "getName(Class) excepts on empty Table tag"() {
		when:
		mapper.getName(EmptyTagged)

		then:
		thrown NonPersistableException
	}

	def "getName(Field) returns name of non-Column-tagged field"() {
		expect:
		mapper.getName(f) == f.getName()

		where:
		f << [NonTagged.getDeclaredField("s")]
	}
	def "getName(Field) returns custom name of Column-tagged field"() {
		expect:
		mapper.getName(f) == name

		where:
		f << [Tagged.getDeclaredField("s")]
		name << ["CustomColumn"]
	}
	def "getName(Field) excepts on empty Column tag"() {
		when:
		mapper.getName(EmptyTagged.getDeclaredField("s"))

		then:
		thrown NonPersistableException
	}
}
