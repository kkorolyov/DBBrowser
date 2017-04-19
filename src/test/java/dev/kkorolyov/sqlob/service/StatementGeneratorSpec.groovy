package dev.kkorolyov.sqlob.service

import dev.kkorolyov.simplelogs.Logger
import dev.kkorolyov.sqlob.annotation.Column
import dev.kkorolyov.sqlob.annotation.Table
import dev.kkorolyov.sqlob.utility.Condition
import groovy.transform.PackageScope
import spock.lang.Specification

import static dev.kkorolyov.sqlob.service.Constants.ID_NAME

class StatementGeneratorSpec extends Specification {
  static {
    Logger.getLogger("dev.kkorolyov.sqlob", Logger.Level.DEBUG, new PrintWriter(System.err));	// Enable logging
  }

  Mapper mapper = Mock()
  StatementGenerator generator = new StatementGenerator()

  def "generateCreate() returns statement for class and per associated class"() {
    expect:
    generator.generateCreate(c).size() == size

    where:
    c << [HasPrimitive, HasComplexes]
    size << [1, 2]
  }

  def "generateInsert() returns statement for class and per complex field"() {
    expect:
    generator.generateInsert(c).size() == size

    where:
    c << [HasPrimitive, HasComplexes]
    size << [1, 4]
  }
  def "generateInsert() returns statement for class last"() {
    expect:
    generator.generateInsert(c).last() == statement

    where:
    c << [HasComplexes]
    statement << ["INSERT INTO TABLE HasComplexes (" + ID_NAME + ", e1, e2, e3) VALUES (?, ?, ?, ?)"]
  }

  def "generateUpdate() uses getName()"() {
    expect:
    generator.generateUpdate(c, new Condition()) == statement

    where:
    c << [NonTagged, Tagged]
    statement << ["UPDATE NonTagged SET s = ? WHERE ",
                  "UPDATE CustomTable SET CustomColumn = ? WHERE "]
  }

  def "generateSelect() uses getName()"() {
    expect:
    generator.generateSelect(c) == statement

    where:
    c << [NonTagged, Tagged]
    statement << ["SELECT * FROM NonTagged",
                  "SELECT * FROM CustomTable"]
  }
  def "generateSelect() appends WHERE if condition not null"() {
    expect:
    generator.generateSelect(c, where) == statement

    where:
    c << [HasPrimitive, HasPrimitive]
    where << [new Condition("s", "==", "test"), null]
    statement << ["SELECT * FROM HasPrimitive WHERE s == ?",
                  "SELECT * FROM HasPrimitive"]
  }
  def "generateSelectId() selects only id field"() {
    expect:
    generator.generateSelectId(c, where) == statement

    where:
    c << [HasPrimitive, HasPrimitive]
    where << [new Condition("s", "==", "s"), null]
    statement << ["SELECT " + ID_NAME + " FROM HasPrimitive WHERE s == ?",
                  "SELECT " + ID_NAME + " FROM HasPrimitive"]
  }

  def "generateDelete() uses getName()"() {
    expect:
    generator.generateDelete(c, null) == statement

    where:
    c << [NonTagged, Tagged]
    statement << ["DELETE FROM NonTagged",
                  "DELETE FROM CustomTable"]
  }
  def "generateDelete() appends WHERE if condition not null"() {
    expect:
    generator.generateDelete(c, where) == statement

    where:
    c << [HasPrimitive]
    where << [new Condition("s", "==", "test")]
    statement << ["DELETE FROM HasPrimitive WHERE s == ?"]
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
  class HasComplexes {
    HasPrimitive e1
    @PackageScope HasPrimitive e2
    private HasPrimitive e3
  }
}
