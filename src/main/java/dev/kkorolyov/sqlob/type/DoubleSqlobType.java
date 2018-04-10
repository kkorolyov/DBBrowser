package dev.kkorolyov.sqlob.type;

import dev.kkorolyov.sqlob.type.BaseSqlobType;

import java.sql.ResultSet;

public class DoubleSqlobType extends BaseSqlobType<Double> {
	public DoubleSqlobType() {
		put(DEFAULT,
				new Delegate<>("DOUBLE PRECISION", ResultSet::getDouble,
						Double.TYPE, Double.class));
	}
}
