package dev.kkorolyov.sqlob.service

import dev.kkorolyov.sqlob.annotation.Column
import dev.kkorolyov.sqlob.annotation.Table
import dev.kkorolyov.sqlob.persistence.NonPersistableException
import groovy.transform.PackageScope
import spock.lang.Shared
import spock.lang.Specification

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class StatementGeneratorSpec extends Specification {
  @Shared Method getNameClass = getGetNameClassMethod()
  @Shared Method getNameField = getGetNameFieldMethod()

  Mapper mapper = Mock()
  StatementGenerator generator = new StatementGenerator(mapper)

  def "generateCreate() returns statement for each associated class"() {
    expect:
    generator.generateCreate()
  }

  def "getName(Class) returns simple name of non-Table-tagged class"() {
    expect:
    getNameClass.invoke(null, c) == name

    where:
    c << [NonTagged]
    name << ["NonTagged"]
  }
  def "getName(Class) returns custom name of Table-tagged class"() {
    expect:
    getNameClass.invoke(null, c) == name

    where:
    c << [Tagged]
    name << ["CustomTable"]
  }
  def "getName(Class) excepts on empty Table tag"() {
    when:
    try {
      getNameClass.invoke(null, EmptyTagged)
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof NonPersistableException) throw new NonPersistableException()
    }
    then:
    thrown NonPersistableException
  }

  def "getName(Field) returns name of non-Column-tagged field"() {
    expect:
    getNameField.invoke(null, f) == name

    where:
    f << [NonTagged.getDeclaredField("s")]
    name << ["s"]
  }
  def "getName(Field) returns custom name of Column-tagged field"() {
    expect:
    getNameField.invoke(null, f) == name

    where:
    f << [Tagged.getDeclaredField("s")]
    name << ["CustomColumn"]
  }
  def "getName(Field) excepts on empty Column tag"() {
    when:
    try {
      getNameField.invoke(null, EmptyTagged.getDeclaredField("s"))
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof NonPersistableException) throw new NonPersistableException()
    }
    then:
    thrown NonPersistableException
  }

  private static Method getGetNameClassMethod() {
    Method getName = StatementGenerator.getDeclaredMethod("getName", Class)
    getName.setAccessible(true)

    getName
  }
  private static Method getGetNameFieldMethod() {
    Method getName = StatementGenerator.getDeclaredMethod("getName", Field)
    getName.setAccessible(true)

    getName
  }

  class NonTagged {
    String s
  }
  @Table("CustomTable") class Tagged {
    @Column("CustomColumn") String s
  }
  @Table("") class EmptyTagged {
    @Column("") String s
  }

  class HasPrimitive {
    private String s
  }
  class HasNonPrimitive {
    HasPrimitive e1
    @PackageScope HasPrimitive e2
    private HasPrimitive e3
  }
}
