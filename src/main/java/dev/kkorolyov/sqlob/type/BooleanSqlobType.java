package dev.kkorolyov.sqlob.type;

import dev.kkorolyov.sqlob.type.BaseSqlobType;

import java.sql.ResultSet;

public class BooleanSqlobType extends BaseSqlobType<Boolean> {
	public BooleanSqlobType() {
		put(DEFAULT,
				new Delegate<>("BOOLEAN", ResultSet::getBoolean,
						Boolean.TYPE, Boolean.class));
	}
}
