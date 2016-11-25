package dev.kkorolyov.sqlob.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Overrides the column a field maps to. 
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface Column {
	/** @return name of the column the annotated field maps to */
	String value();
}
