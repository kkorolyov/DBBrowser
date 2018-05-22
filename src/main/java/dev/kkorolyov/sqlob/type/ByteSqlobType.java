package dev.kkorolyov.sqlob.type;

import dev.kkorolyov.sqlob.type.BaseSqlobType;

import java.sql.ResultSet;

public class ByteSqlobType extends BaseSqlobType<Byte> {
	public ByteSqlobType() {
		put(DEFAULT,
				new Delegate<>("TINYINT", ResultSet::getByte,
						Byte.TYPE, Byte.class));
	}
}
