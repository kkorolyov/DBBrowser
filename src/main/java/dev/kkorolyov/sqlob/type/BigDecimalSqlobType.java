package dev.kkorolyov.sqlob.type;

import dev.kkorolyov.sqlob.type.BaseSqlobType;

import java.math.BigDecimal;
import java.sql.ResultSet;

public class BigDecimalSqlobType extends BaseSqlobType<BigDecimal> {
	public BigDecimalSqlobType() {
		put(DEFAULT,
				new Delegate<>("NUMERIC", ResultSet::getBigDecimal,
						BigDecimal.class));
	}
}
