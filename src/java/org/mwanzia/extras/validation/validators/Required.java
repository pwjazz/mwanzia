package org.mwanzia.extras.validation.validators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.sf.oval.configuration.annotation.Constraint;

/**
 * Validate that the value is not null and, if it is a String, is not empty.
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Constraint(checkWith = RequiredCheck.class)
public @interface Required {
    public String errorCode() default "org.mwanzia.extras.validation.validators.Required";

    public String message() default "org.mwanzia.extras.validation.validators.Required.violated";
}
