package dev.kkorolyov.sqlob.column

class ReferencingColumnSpec extends BaseColumnSpec {
	@Override
	Column<?> getTestTarget() {
		return Spy(ReferencingColumn, constructorArgs: [f])
	}
}
