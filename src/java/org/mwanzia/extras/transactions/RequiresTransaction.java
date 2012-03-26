package org.mwanzia.extras.transactions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a class or a method as requiring a transaction. If applied to a class,
 * all methods in that class (and its sub-classes) will automatically get a
 * transaction.
 * 
 * @author percy wegmann ( percy <at> karen and percy <dot> net )
 * 
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresTransaction {

}
