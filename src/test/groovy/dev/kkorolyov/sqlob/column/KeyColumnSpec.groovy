package dev.kkorolyov.sqlob.column

import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.type.SqlobType

import spock.lang.Specification

import java.sql.DatabaseMetaData

import static dev.kkorolyov.simplespecs.SpecUtilities.randString
import static dev.kkorolyov.simplespecs.SpecUtilities.setField

class KeyColumnSpec extends Specification {
	ExecutionContext context = Mock()
	DatabaseMetaData metaData = Mock()

	def "primary sql is primary key"() {
		String name = randString()
		String sqlType = randString()
		SqlobType<UUID> sqlobType = Mock()

		KeyColumn column = KeyColumn.primary(name).with {
			setField("sqlobType", Column, it, sqlobType)
			it
		}
		when:
		String sql = column.getSql(context)

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.getSqlType(metaData) >> sqlType
		sql == "$name $sqlType PRIMARY KEY"
	}

	def "foreign sql is foreign key"() {
		String name = randString()
		String referencedName = randString()
		String sqlType = randString()
		SqlobType<UUID> sqlobType = Mock()

		KeyColumn column = KeyColumn.foreign(name, referencedName).with {
			setField("sqlobType", Column, it, sqlobType)
			it
		}
		when:
		String sql = column.getSql(context)

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.getSqlType(metaData) >> sqlType
		sql == "$name $sqlType, FOREIGN KEY ($name) REFERENCES $referencedName(${KeyColumn.ID.getName()}) ON DELETE SET NULL"
	}
}
