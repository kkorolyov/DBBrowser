package dev.kkorolyov.sqlob.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates the name of the table a class maps to.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Table {
	/** @return name of the table the annotated class maps to */
	String value();
}
