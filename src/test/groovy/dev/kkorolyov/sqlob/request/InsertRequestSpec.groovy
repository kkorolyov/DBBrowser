package dev.kkorolyov.sqlob.request

import dev.kkorolyov.sqlob.Stub
import dev.kkorolyov.sqlob.result.ConfigurableRecord
import dev.kkorolyov.sqlob.result.Record

import static dev.kkorolyov.simplespecs.SpecUtilities.randString

class InsertRequestSpec extends BaseRequestSpec<InsertRequest<?>> {
	Collection<Record<?, ?>> records = (0..5).collect {
		new ConfigurableRecord<>(UUID.randomUUID(), Stub.BasicStub.random())
	}

	InsertRequest<?> request = Spy(InsertRequest, constructorArgs: [records, randString(), columns])

	def "inserts or updates non-existent records"() {
		def (Collection<Record<?>> ignored, Collection<Record<?>> updating, Collection<Record<?>> remaining) = records.collate(records.size() / 3 as int)

		// TODO
	}
}
