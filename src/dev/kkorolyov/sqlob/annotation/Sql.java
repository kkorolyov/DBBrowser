package dev.kkorolyov.sqlob.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates the associated table name if marking a class, or the associated SQL type if marking a field.
 */
@Target({TYPE, FIELD})
@Retention(RUNTIME)
public @interface Sql {
	/** @return SQL type mapped to the field with this annotation */
	String value();
}
