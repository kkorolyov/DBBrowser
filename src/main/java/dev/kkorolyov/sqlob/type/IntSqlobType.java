package dev.kkorolyov.sqlob.type;

import dev.kkorolyov.sqlob.type.BaseSqlobType;

import java.sql.ResultSet;

public class IntSqlobType extends BaseSqlobType<Integer> {
	public IntSqlobType() {
		put(DEFAULT,
				new Delegate<>("INTEGER", ResultSet::getInt,
						Integer.TYPE, Integer.class));
	}
}
