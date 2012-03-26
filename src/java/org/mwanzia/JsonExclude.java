package org.mwanzia;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Marks a class or individual property (getter method) as excluded from
 * delivery to client via JSON.
 * <p>
 * 
 * @author percy
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
public @interface JsonExclude {

}
