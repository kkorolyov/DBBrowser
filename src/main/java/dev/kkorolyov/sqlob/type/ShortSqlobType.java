package dev.kkorolyov.sqlob.type;

import java.sql.ResultSet;

public class ShortSqlobType extends BaseSqlobType<Short> {
	public ShortSqlobType() {
		put(DEFAULT,
				new Delegate<>("SMALLINT", ResultSet::getShort,
						Short.TYPE, Short.class));
	}
}
