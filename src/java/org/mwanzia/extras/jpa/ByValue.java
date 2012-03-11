package org.mwanzia.extras.jpa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a particular parameter is allowed to be passed by value. By
 * default, parameters that are managed entities are not allowed to be passed by
 * value, but this annotation allows that to be overridden.
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ByValue {

}
