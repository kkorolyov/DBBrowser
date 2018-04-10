package dev.kkorolyov.sqlob.type;

import dev.kkorolyov.sqlob.type.BaseSqlobType;

import java.sql.ResultSet;

public class LongSqlobType extends BaseSqlobType<Long> {
	public LongSqlobType() {
		put(DEFAULT,
				new Delegate<>("BIGINT", ResultSet::getLong,
						Long.TYPE, Long.class));
	}
}
