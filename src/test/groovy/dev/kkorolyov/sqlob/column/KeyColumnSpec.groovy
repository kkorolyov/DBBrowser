package dev.kkorolyov.sqlob.column

import dev.kkorolyov.simplefiles.Providers
import dev.kkorolyov.sqlob.ExecutionContext
import dev.kkorolyov.sqlob.result.ConfigurableRecord
import dev.kkorolyov.sqlob.type.SqlobType
import dev.kkorolyov.sqlob.type.factory.SqlobTypeFactory

import spock.lang.Specification

import java.sql.DatabaseMetaData
import java.sql.ResultSet

import static dev.kkorolyov.simplespecs.SpecUtilities.getField
import static dev.kkorolyov.simplespecs.SpecUtilities.randString
import static dev.kkorolyov.simplespecs.SpecUtilities.setField

class KeyColumnSpec extends Specification {
	static final Providers<SqlobType> ORIGINAL_SQLOB_TYPES = getField("SQLOB_TYPES", SqlobTypeFactory)

	ExecutionContext context = Mock()
	DatabaseMetaData metaData = Mock()

	String name = randString()
	SqlobType<UUID> sqlobType = Mock() {
		getTypes() >> [UUID]
	}

	UUID key = UUID.randomUUID()
	ConfigurableRecord<UUID, ?> record = new ConfigurableRecord<>(key, null)

	def cleanupSpec() {
		setField("SQLOB_TYPES", SqlobTypeFactory, ORIGINAL_SQLOB_TYPES)
	}

	def setup() {
		setField("SQLOB_TYPES", SqlobTypeFactory, Providers.fromInstances(SqlobType, [sqlobType] as Set))
	}

	def "gets record key"() {
		when:
		Object result = KeyColumn.primary(name).get(record, context)

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.get(metaData, key) >> key
		result == key
	}
	def "sets resultSet value as record key"() {
		ResultSet rs = Mock()
		UUID newKey = UUID.randomUUID()

		when:
		KeyColumn.primary(name).set(record, rs, context)

		then:
		1 * context.metadata >> metaData
		1 * sqlobType.get(metaData, rs, name) >> newKey
		record.key == newKey
	}

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
		sql == "$name $sqlType REFERENCES $referencedName(${KeyColumn.ID.getName()}) ON DELETE SET NULL"
	}
}
