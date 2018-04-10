package dev.kkorolyov.sqlob.type;

import dev.kkorolyov.sqlob.type.BaseSqlobType;

import java.sql.ResultSet;

public class FloatSqlobType extends BaseSqlobType<Float> {
	public FloatSqlobType() {
		put(DEFAULT,
				new Delegate<>("REAL", ResultSet::getFloat,
						Float.TYPE, Float.class));
	}
}
