package dev.kkorolyov.sqlob.service

import dev.kkorolyov.sqlob.annotation.Transient
import groovy.transform.PackageScope
import spock.lang.Shared
import spock.lang.Specification

import java.lang.reflect.Field
import java.sql.ResultSet

class MapperSpec extends Specification {
  @Shared String stubSqlType = "SomeSQL"
  @Shared Closure<?> stubExtractor = {ResultSet.getString}

  Mapper mapper = new Mapper()

  def "getPersistableFields() returns one of each persistable field"() {
    expect:
    Iterable<Field> results = mapper.getPersistableFields(c)

    results.containsAll(fields)
    results.size() == size

    where:
    c << [Multi]
    fields << [[Multi.getDeclaredField("e1"), Multi.getDeclaredField("e2"), Multi.getDeclaredField("e3")]]
    size << [3]
  }

  def "getPersistableFields() ignores Transient-tagged fields"() {
    expect:
    mapper.getPersistableFields(c).size() == size

    where:
    c << [TransientTag, TransientTagPlusOne]
    size << [0, 1]
  }
  def "getPersistableFields() ignores transient fields"() {
    expect:
    mapper.getPersistableFields(c).size() == size

    where:
    c << [TransientModifier, TransientModifierPlusOne]
    size << [0, 1]
  }
  def  "getPersistedFields() ignores static fields"() {
    expect:
    mapper.getPersistableFields(c).size() == size

    where:
    c << [StaticModifier, StaticModifierPlusOne]
    size << [0, 1]
  }

  def "getAssociatedClasses() returns one of each class"() {
    expect:
    Iterable<Class<?>> results = mapper.getAssociatedClasses(c)

    results.containsAll(classes)
    results.size() == size

    where:
    c << [Multi, SelfRef, RefLoop1, RefLoop2]
    classes << [[Multi, Empty],
                SelfRef,
                [RefLoop1, RefLoop2],
                [RefLoop1, RefLoop2]]
    size << [2, 1, 2, 2]
  }

  def "put() uses sanitized sql type"() {
    Class<?> c = Empty
    String sqlType = stubSqlType

    when:
    mapper.put(c, sqlType, stubExtractor)

    then:
    mapper.getSql(c) == Constants.sanitize(sqlType)
  }

  def "typemapped classes are primitive"() {
    Class<?> c = Empty

    when:
    mapper.put(c, stubSqlType, stubExtractor)

    then:
    mapper.isPrimitive(c)
    !mapper.isComplex(c)
  }
  def "non-typemapped classes are complex"() {
    Class<?> c = Empty

    expect:
    mapper.isComplex(c)
    !mapper.isPrimitive(c)
  }

  def "fields of typemapped class are primitive"() {
    when:
    mapper.put(Empty, stubSqlType, stubExtractor)

    then:
    mapper.isPrimitive(f)
    !mapper.isComplex(f)

    where:
    f << [Multi.getDeclaredField("e1"), Multi.getDeclaredField("e2"), Multi.getDeclaredField("e3")]
  }
  def "fields of non-typemapped class are complex"() {
    expect:
    mapper.isComplex(f)
    !mapper.isPrimitive(f)

    where:
    f << [Multi.getDeclaredField("e1"), Multi.getDeclaredField("e2"), Multi.getDeclaredField("e3")]
  }

  class Empty {}

  class TransientTag {
    @Transient
    private Empty e
  }
  class TransientTagPlusOne {
    @Transient
    private Empty e1
    private Empty e2
  }

  class TransientModifier {
    private transient Empty e
  }
  class TransientModifierPlusOne {
    private transient Empty e1
    private Empty e2
  }

  class StaticModifier {
    private static Empty e
  }
  class StaticModifierPlusOne {
    private static Empty e1
    private Empty e2
  }

  class Multi {
    Empty e1
    @PackageScope Empty e2
    private Empty e3
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
}
