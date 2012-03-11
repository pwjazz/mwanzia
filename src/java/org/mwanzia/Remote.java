package org.mwanzia;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Marks a method as being remotely executable. Optionally, one may specify the
 * names of the Applications in which this method is remotely executable.
 * </p>
 * 
 * @author percy
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.METHOD })
public @interface Remote {
	String[] applications() default {};
}
