package dev.kkorolyov.sqlob.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that a field should be ignored by the persistence engine.
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface Transient {
	// Tag
}
