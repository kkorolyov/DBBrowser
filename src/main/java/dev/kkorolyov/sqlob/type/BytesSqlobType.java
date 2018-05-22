package dev.kkorolyov.sqlob.type;

import dev.kkorolyov.sqlob.type.BaseSqlobType;

import java.sql.ResultSet;

public class BytesSqlobType extends BaseSqlobType<byte[]> {
	public BytesSqlobType() {
		put(DEFAULT,
				new Delegate<>("VARBINARY(1024)", ResultSet::getBytes,
						byte[].class));
	}
}
